package com.Liang.java.ai.langchain4j.release;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalValidationEnvironmentTest {

    @Test
    void acceptsOnlyTemporaryValidationDatabaseNames() {
        assertThat(ExternalValidationEnvironment.requireValidationJdbcUrl(
                "jdbc:mysql://localhost:3306/ai_medical_care_release_validation_abc?useSSL=false"))
                .contains("ai_medical_care_release_validation_abc");

        assertThatThrownBy(() -> ExternalValidationEnvironment.requireValidationJdbcUrl(
                "jdbc:mysql://localhost:3306/guiguxiaozhi?useSSL=false"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void generatedRedisNamesStayWithinValidationPrefix() {
        ExternalValidationEnvironment.RedisResourceNames names =
                ExternalValidationEnvironment.newRedisResourceNames();

        assertThat(names.indexName()).startsWith(ExternalValidationEnvironment.VALIDATION_PREFIX);
        assertThat(names.prefix()).startsWith(ExternalValidationEnvironment.VALIDATION_PREFIX);
        assertThat(names.indexName())
                .matches("ai_medical_care_release_validation_index_[0-9a-f]{32}");
        assertThat(names.prefix())
                .matches("ai_medical_care_release_validation_vector:[0-9a-f]{32}:");
    }

    @Test
    void rejectsMalformedOrUnsafeRedisNamesIncludingNullFields() {
        assertThatThrownBy(() -> ExternalValidationEnvironment.requireSafeRedisResources(
                new ExternalValidationEnvironment.RedisResourceNames(null, "valid")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ExternalValidationEnvironment.requireSafeRedisResources(
                new ExternalValidationEnvironment.RedisResourceNames("xiaozhi-index", "langchain4j:vector:xiaozhi:")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ExternalValidationEnvironment.requireSafeValidationName("release_validation_trigger"))
                .isInstanceOf(IllegalStateException.class);
    }
}
