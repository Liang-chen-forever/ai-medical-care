package com.liang.medical.release;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Environment and resource-safety helpers shared by release-only integration tests. */
final class ExternalValidationEnvironment {

    static final String VALIDATION_PREFIX = "ai_medical_care_release_validation_";
    static final String SAFE_VALIDATION_JDBC_URL = "jdbc:mysql://192.0.2.1:3306/ai_medical_care_release_validation_unconfigured?connectTimeout=100&socketTimeout=100";
    private static final Pattern JDBC_DATABASE = Pattern.compile("^jdbc:[^:]+://[^/]+/([^?;]+)(?:\\?[^#;]*)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATABASE_NAME = Pattern.compile("^" + Pattern.quote(VALIDATION_PREFIX) + "\\d{8}_\\d{6}_[0-9a-f]{8}$");
    private static final Pattern REDIS_INDEX_NAME = Pattern.compile("^" + Pattern.quote(VALIDATION_PREFIX) + "index_[0-9a-f]{32}$");
    private static final Pattern REDIS_VECTOR_PREFIX = Pattern.compile("^" + Pattern.quote(VALIDATION_PREFIX) + "vector:[0-9a-f]{32}:$");
    // MySQL limits trigger identifiers to 64 characters; this format is 63 characters.
    private static final Pattern TRIGGER_NAME = Pattern.compile("^" + Pattern.quote(VALIDATION_PREFIX) + "trigger_[0-9a-f]{20}$");

    private ExternalValidationEnvironment() {
    }

    static String requireValidationJdbcUrl() {
        return requireValidationJdbcUrl(requireEnvironment("RELEASE_VALIDATION_JDBC_URL"));
    }

    static String requireValidationJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalStateException("RELEASE_VALIDATION_JDBC_URL is required");
        }
        if (!jdbcUrl.equals(jdbcUrl.trim())) {
            throw new IllegalStateException("release validation JDBC URL must not contain surrounding whitespace");
        }
        Matcher matcher = JDBC_DATABASE.matcher(jdbcUrl);
        if (!matcher.matches() || !DATABASE_NAME.matcher(matcher.group(1)).matches()) {
            throw new IllegalStateException("release validation JDBC URL must target a temporary validation database");
        }
        return jdbcUrl;
    }

    static String requireDbUser() {
        return requireEnvironment("RELEASE_VALIDATION_DB_USER");
    }

    static String requireDbPassword() {
        return requireEnvironment("RELEASE_VALIDATION_DB_PASSWORD");
    }

    static String requireRedisHost() {
        return requireEnvironment("RELEASE_VALIDATION_REDIS_HOST");
    }

    static int requireRedisPort() {
        String value = requireEnvironment("RELEASE_VALIDATION_REDIS_PORT");
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65535) {
                throw new NumberFormatException("out of range");
            }
            return port;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("RELEASE_VALIDATION_REDIS_PORT must be a valid TCP port", exception);
        }
    }

    static boolean isConfigured() {
        return hasEnvironment("RELEASE_VALIDATION_JDBC_URL")
                && hasEnvironment("RELEASE_VALIDATION_DB_USER")
                && hasEnvironment("RELEASE_VALIDATION_DB_PASSWORD")
                && hasEnvironment("RELEASE_VALIDATION_REDIS_HOST")
                && hasEnvironment("RELEASE_VALIDATION_REDIS_PORT");
    }

    static RedisResourceNames newRedisResourceNames() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        return new RedisResourceNames(VALIDATION_PREFIX + "index_" + suffix,
                VALIDATION_PREFIX + "vector:" + suffix + ":");
    }

    static String newValidationTriggerName() {
        return VALIDATION_PREFIX + "trigger_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    static void requireSafeValidationName(String name) {
        if (name == null || !(DATABASE_NAME.matcher(name).matches()
                || REDIS_INDEX_NAME.matcher(name).matches()
                || REDIS_VECTOR_PREFIX.matcher(name).matches()
                || TRIGGER_NAME.matcher(name).matches())) {
            throw new IllegalStateException("refusing to operate on a non-validation resource");
        }
    }

    static void requireSafeTriggerName(String name) {
        if (name == null || !TRIGGER_NAME.matcher(name).matches()) {
            throw new IllegalStateException("refusing to operate on a non-validation trigger");
        }
    }

    static void requireSafeRedisResources(RedisResourceNames names) {
        if (names == null || names.indexName() == null || names.prefix() == null) {
            throw new IllegalStateException("refusing to operate on a non-validation Redis resource");
        }
        try {
            if (!REDIS_INDEX_NAME.matcher(names.indexName()).matches()
                    || !REDIS_VECTOR_PREFIX.matcher(names.prefix()).matches()) {
                throw new IllegalStateException("invalid Redis resource format");
            }
        } catch (IllegalStateException exception) {
            throw new IllegalStateException("refusing to operate on a non-validation Redis resource");
        }
    }

    private static String requireEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }

    private static boolean hasEnvironment(String name) {
        String value = System.getenv(name);
        return value != null && !value.isBlank();
    }

    record RedisResourceNames(String indexName, String prefix) {
    }
}
