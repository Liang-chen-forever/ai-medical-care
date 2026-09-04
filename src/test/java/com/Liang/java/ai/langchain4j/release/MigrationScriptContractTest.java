package com.Liang.java.ai.langchain4j.release;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationScriptContractTest {

    private static final List<String> MIGRATION_FILES = List.of(
            "db/migration/V2__secure_appointments.sql",
            "db/migration/V3__roles_and_doctor_accounts.sql",
            "db/migration/V4__appointment_lifecycle.sql",
            "db/migration/V5__triage_cases.sql"
    );

    private static final List<String> SCHEMA_AWARE_MIGRATIONS = List.of(
            "db/migration/V2__secure_appointments.sql",
            "db/migration/V3__roles_and_doctor_accounts.sql",
            "db/migration/V4__appointment_lifecycle.sql"
    );

    private static final List<String> EXPECTED_INDEXES = List.of(
            "uk_appointment_user_schedule",
            "idx_appointment_user_id",
            "idx_appointment_schedule_id",
            "uk_doctor_user_id",
            "idx_appointment_doctor_status",
            "idx_appointment_user_status",
            "idx_triage_case_patient_created",
            "uk_triage_evidence_case_rank"
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
}
