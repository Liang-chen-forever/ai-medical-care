package com.Liang.java.ai.langchain4j.release;

import com.Liang.java.ai.langchain4j.knowledge.KnowledgeSeedCatalog;
import com.Liang.java.ai.langchain4j.knowledge.KnowledgeSeedLoader;
import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("external")
class KnowledgeRedisExternalIntegrationTest {

    @Test
    void reloadsSeedIntoIsolatedRedisIndexAndCleansItUp() {
        Assumptions.assumeTrue(ExternalValidationEnvironment.isConfigured());
        ExternalValidationEnvironment.RedisResourceNames names =
                ExternalValidationEnvironment.newRedisResourceNames();
        ExternalValidationEnvironment.requireSafeRedisResources(names);

        RedisEmbeddingStore store = null;
        Throwable primaryFailure = null;
        try {
            store = RedisEmbeddingStore.builder()
                    .host(ExternalValidationEnvironment.requireRedisHost())
                    .port(ExternalValidationEnvironment.requireRedisPort())
                    .indexName(names.indexName())
                    .prefix(names.prefix())
                    .dimension(8)
                    .build();
            EmbeddingModel localModel = new DeterministicEightDimensionModel();
            KnowledgeSeedLoader loader = new KnowledgeSeedLoader(new KnowledgeSeedCatalog(), store, localModel);

            assertThat(loader.reload().documentsLoaded()).isPositive();
            List<EmbeddingMatch<TextSegment>> matches = store.search(EmbeddingSearchRequest.builder()
                    .queryEmbedding(Embedding.from(new float[]{1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F}))
                    .maxResults(1)
                    .build()).matches();
            assertThat(matches).isNotEmpty();
            TextSegment seed = matches.get(0).embedded();
            assertThat(seed.metadata().getString(KnowledgeSeedCatalog.DOCUMENT_ID))
                    .isIn("hospital-overview", "department-overview", "neurology-overview", "dentistry-overview");
            assertThat(seed.metadata().getString(KnowledgeSeedCatalog.KNOWLEDGE_VERSION)).isEqualTo("2026.09");
        } catch (Throwable failure) {
            primaryFailure = failure;
            throwUnchecked(failure);
            return;
        } finally {
            Throwable cleanupFailure = null;
            try {
                if (store != null) {
                    store.close();
                }
            } catch (Throwable exception) {
                cleanupFailure = exception;
            }
            try {
                ExternalValidationEnvironment.requireSafeRedisResources(names);
                try (JedisPooled redis = new JedisPooled(
                        ExternalValidationEnvironment.requireRedisHost(),
                        ExternalValidationEnvironment.requireRedisPort())) {
                    try {
                        redis.ftDropIndexDD(names.indexName());
                    } catch (JedisDataException exception) {
                        if (!isMissingIndex(exception)) {
                            throw exception;
                        }
                    }
                }
            } catch (Throwable exception) {
                if (cleanupFailure == null) {
                    cleanupFailure = exception;
                } else {
                    cleanupFailure.addSuppressed(exception);
                }
            }
            if (primaryFailure != null && cleanupFailure != null) {
                primaryFailure.addSuppressed(cleanupFailure);
            } else if (cleanupFailure != null) {
                throwUnchecked(cleanupFailure);
            }
        }
    }

    private static boolean isMissingIndex(JedisDataException exception) {
        String message = exception.getMessage();
        return message != null && message.toLowerCase().contains("unknown index");
    }

    private static void throwUnchecked(Throwable exception) {
        KnowledgeRedisExternalIntegrationTest.<RuntimeException>rethrow(exception);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void rethrow(Throwable exception) throws E {
        throw (E) exception;
    }

    private static final class DeterministicEightDimensionModel implements EmbeddingModel {
        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
            return Response.from(segments.stream()
                    .map(segment -> Embedding.from(new float[]{1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F}))
                    .toList());
        }

        @Override
        public int dimension() {
            return 8;
        }
    }
}
