package com.liang.medical.release;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeRedisExternalIntegrationContractTest {

    private static final Path EXTERNAL_TEST = Path.of("src", "test", "java", "com", "liang", "medical",
            "release", "KnowledgeRedisExternalIntegrationTest.java");

    @Test
    void cleanupBoundaryCoversStoreCreationAndPreservesPrimaryFailure() throws IOException {
        String source = Files.readString(EXTERNAL_TEST, StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
        int outerTry = source.indexOf("RedisEmbeddingStore store = null;\n        Throwable primaryFailure = null;\n        try {");
        int builder = source.indexOf("RedisEmbeddingStore.builder()");
        int outerFinally = source.indexOf("        } finally {", builder);

        assertThat(outerTry).isNotNegative().isLessThan(builder);
        assertThat(outerFinally).isGreaterThan(builder);
        assertThat(source)
                .contains("ExternalValidationEnvironment.requireSafeRedisResources(names);")
                .contains("isMissingIndex(exception)")
                .contains("primaryFailure.addSuppressed(cleanupFailure)")
                .contains(".metadataKeys(KnowledgeSeedCatalog.metadataKeys())");
    }
}
