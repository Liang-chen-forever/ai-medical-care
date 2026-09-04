package com.Liang.java.ai.langchain4j.release;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Environment and resource-safety helpers shared by release-only integration tests. */
final class ExternalValidationEnvironment {

    static final String VALIDATION_PREFIX = "ai_medical_care_release_validation_";
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

    static void requireSafeRedisResources(RedisResourceNames names) {
        if (names == null
                || !names.indexName().startsWith(VALIDATION_PREFIX)
                || !names.prefix().startsWith(VALIDATION_PREFIX)
                || names.indexName().equals("xiaozhi-index")
                || names.prefix().equals("langchain4j:vector:xiaozhi:")) {
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
