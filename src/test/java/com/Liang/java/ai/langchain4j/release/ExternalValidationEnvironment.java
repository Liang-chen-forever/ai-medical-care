package com.Liang.java.ai.langchain4j.release;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Environment and resource-safety helpers shared by release-only integration tests. */
final class ExternalValidationEnvironment {

    static final String VALIDATION_PREFIX = "ai_medical_care_release_validation_";
    static final String SAFE_VALIDATION_JDBC_URL = "jdbc:mysql://192.0.2.1:3306/ai_medical_care_release_validation_unconfigured?connectTimeout=100&socketTimeout=100";
    private static final Pattern JDBC_DATABASE = Pattern.compile("^jdbc:[^:]+://[^/]+/([^?;]+)", Pattern.CASE_INSENSITIVE);

    private ExternalValidationEnvironment() {
    }

    static String requireValidationJdbcUrl() {
        return requireValidationJdbcUrl(requireEnvironment("RELEASE_VALIDATION_JDBC_URL"));
    }

    static String requireValidationJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalStateException("RELEASE_VALIDATION_JDBC_URL is required");
        }
        Matcher matcher = JDBC_DATABASE.matcher(jdbcUrl.trim());
        if (!matcher.find() || !matcher.group(1).startsWith(VALIDATION_PREFIX)) {
            throw new IllegalStateException("release validation JDBC URL must target a temporary validation database");
        }
        return jdbcUrl.trim();
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
        return VALIDATION_PREFIX + "trigger_" + UUID.randomUUID().toString().replace("-", "");
    }

    static void requireSafeValidationName(String name) {
        if (name == null || !name.startsWith(VALIDATION_PREFIX) || name.length() == VALIDATION_PREFIX.length()) {
            throw new IllegalStateException("refusing to operate on a non-validation resource");
        }
    }

    static void requireSafeRedisResources(RedisResourceNames names) {
        if (names == null || names.indexName() == null || names.prefix() == null) {
            throw new IllegalStateException("refusing to operate on a non-validation Redis resource");
        }
        try {
            requireSafeValidationName(names.indexName());
            requireSafeValidationName(names.prefix());
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
