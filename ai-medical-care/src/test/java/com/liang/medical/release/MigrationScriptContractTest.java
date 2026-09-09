package com.liang.medical.release;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationScriptContractTest {

    private static final Path RELEASE_RUNNER = Path.of("..", "scripts", "release-validation.ps1");
    private static final Path SCHEMA_BASELINE = Path.of("..", "scripts", "release-validation", "schema-baseline.sql");

    private static final List<String> MIGRATION_FILES = List.of(
            "db/migration/V2__secure_appointments.sql",
            "db/migration/V3__roles_and_doctor_accounts.sql",
            "db/migration/V4__appointment_lifecycle.sql",
            "db/migration/V5__triage_cases.sql",
            "db/migration/V6__waitlist_encounter_audit.sql",
            "db/migration/V7__knowledge_documents.sql"
    );

    private static final List<String> SCHEMA_AWARE_MIGRATIONS = List.of(
            "db/migration/V2__secure_appointments.sql",
            "db/migration/V3__roles_and_doctor_accounts.sql",
            "db/migration/V4__appointment_lifecycle.sql",
            "db/migration/V6__waitlist_encounter_audit.sql",
            "db/migration/V7__knowledge_documents.sql"
    );

    private static final List<String> EXPECTED_INDEXES = List.of(
            "uk_appointment_user_schedule",
            "idx_appointment_user_id",
            "idx_appointment_schedule_id",
            "uk_doctor_user_id",
            "idx_appointment_doctor_status",
            "idx_appointment_user_status",
            "idx_triage_case_patient_created",
            "uk_triage_evidence_case_rank",
            "idx_appointment_triage_case",
            "idx_waitlist_schedule_status_priority",
            "idx_waitlist_patient_created",
            "uk_encounter_appointment",
            "idx_encounter_patient_completed",
            "idx_audit_trace_created",
            "uk_knowledge_document_hash",
            "uk_knowledge_document_key_version"
    );

    @Test
    void migrationsDoNotSelectAFixedSchema() {
        Map<String, String> migrations = loadMigrations(MIGRATION_FILES);

        assertThat(migrations.values())
                .allSatisfy(script -> assertThat(script).doesNotContain("USE guiguxiaozhi"));
    }

    @Test
    void schemaAwareMigrationsUseTheSelectedDatabase() {
        Map<String, String> migrations = loadMigrations(SCHEMA_AWARE_MIGRATIONS);

        assertThat(migrations.values()).allSatisfy(script ->
                assertThat(script).contains("DATABASE()"));
    }

    @Test
    void migrationsKeepTheExpectedGuardedIndexes() {
        String scripts = String.join("\n", loadMigrations(MIGRATION_FILES).values());

        assertThat(EXPECTED_INDEXES).allSatisfy(index -> assertThat(scripts).contains(index));
    }

    @Test
    void releaseRunnerGuardsDatabaseCreationAndCleanupWithTheExactValidationPrefix() {
        String runner = loadProjectFile(RELEASE_RUNNER);
        String guardInvocation = "Assert-ValidationDatabaseName -DatabaseName $DatabaseName";

        assertThat(runner)
                .contains("$ValidationDatabasePrefix = 'ai_medical_care_release_validation_'")
                .contains("[regex]::Escape($ValidationDatabasePrefix)")
                .contains("$DatabaseName = $ValidationDatabasePrefix + $RunId")
                .contains("function Assert-ValidationDatabaseName")
                .contains("$ValidationDatabasePattern")
                .doesNotContain("guiguxiaozhi", "xiaozhi-index", "langchain4j:vector:xiaozhi:");
        assertThat(countOccurrences(runner, guardInvocation)).isGreaterThanOrEqualTo(2);
        assertThat(runner.indexOf(guardInvocation)).isLessThan(runner.indexOf("CREATE DATABASE"));
        assertThat(runner.lastIndexOf(guardInvocation)).isLessThan(runner.indexOf("DROP DATABASE IF EXISTS"));
        assertThat(runner).contains("$ValidationDatabasePattern = '^' + [regex]::Escape($ValidationDatabasePrefix) + '\\d{8}_\\d{6}_[0-9a-f]{8}$'");
    }

    @Test
    void releaseRunnerCleansUpAfterEveryCreateAttemptWithAValidatedDrop() {
        String runner = loadProjectFile(RELEASE_RUNNER);

        assertThat(runner)
                .contains("$DatabaseCreateAttempted = $false")
                .contains("$DatabaseCreateAttempted = $true")
                .contains("DROP DATABASE IF EXISTS");
        assertThat(runner.indexOf("$DatabaseCreateAttempted = $true"))
                .isLessThan(runner.indexOf("Invoke-MySqlSql -Sql $createSql"));
        assertThat(runner.indexOf("if ($DatabaseCreateAttempted)"))
                .isLessThan(runner.lastIndexOf("DROP DATABASE IF EXISTS"));
    }

    @Test
    void releaseRunnerAppliesEveryMigrationTwiceAndChecksExpectedIndexes() {
        String runner = loadProjectFile(RELEASE_RUNNER);

        assertThat(MIGRATION_FILES).allSatisfy(path ->
                assertThat(runner).contains(Path.of(path).getFileName().toString()));
        assertThat(runner).contains("for ($Pass = 1; $Pass -le 2; $Pass++)");
        assertThat(EXPECTED_INDEXES).allSatisfy(index -> assertThat(runner).contains(index));
    }

    @Test
    void releaseRunnerFeedsExternalTestsAndWritesIgnoredEvidenceRecords() {
        String runner = loadProjectFile(RELEASE_RUNNER);

        assertThat(runner)
                .contains("-Pexternal-integration-tests")
                .contains("RELEASE_VALIDATION_JDBC_URL =")
                .contains("RELEASE_VALIDATION_DB_USER =")
                .contains("RELEASE_VALIDATION_DB_PASSWORD =")
                .contains("RELEASE_VALIDATION_REDIS_HOST =")
                .contains("RELEASE_VALIDATION_REDIS_PORT =")
                .contains("docs/verification/runs/")
                .contains("${DatabaseName}?useSSL=false");
    }

    @Test
    void releaseRunnerDrainsChildStreamsConcurrentlyAndNeverEmitsRawSecrets() {
        String runner = loadProjectFile(RELEASE_RUNNER);

        assertThat(runner)
                .contains("ReadToEndAsync()")
                .contains("GetAwaiter().GetResult()")
                .contains("Get-SanitizedFailureMessage")
                .contains("$startInfo.Environment.Remove")
                .doesNotContain("`n$stderr", "+ $stderr", "`n$stdout");
        assertThat(runner.indexOf("ReadToEndAsync()"))
                .isLessThan(runner.indexOf("WaitForExit()"));
        assertThat(runner).contains("$Message.Replace($DbPassword, '[REDACTED]')");
    }

    @Test
    void releaseBaselineDefinesOnlyTheMinimumLegacyTables() {
        String baseline = loadProjectFile(SCHEMA_BASELINE);

        assertThat(baseline)
                .contains("CREATE TABLE `user`")
                .contains("CREATE TABLE doctor")
                .contains("CREATE TABLE schedule")
                .contains("CREATE TABLE appointment")
                .doesNotContain("guiguxiaozhi");
    }

    private static Map<String, String> loadMigrations(List<String> paths) {
        return paths.stream().collect(java.util.stream.Collectors.toMap(path -> path, MigrationScriptContractTest::loadMigration));
    }

    private static String loadMigration(String path) {
        try (InputStream stream = MigrationScriptContractTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("migration resource %s", path).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new AssertionError("Unable to read migration resource " + path, exception);
        }
    }

    private static String loadProjectFile(Path path) {
        assertThat(path).as("project file %s", path).exists().isRegularFile();
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new AssertionError("Unable to read project file " + path, exception);
        }
    }

    private static int countOccurrences(String value, String expected) {
        return (value.length() - value.replace(expected, "").length()) / expected.length();
    }
}
