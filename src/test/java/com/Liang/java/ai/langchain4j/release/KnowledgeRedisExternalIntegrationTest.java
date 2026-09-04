package com.Liang.java.ai.langchain4j.release;

import com.Liang.java.ai.langchain4j.knowledge.KnowledgeSeedCatalog;
import com.Liang.java.ai.langchain4j.knowledge.KnowledgeSeedLoader;
import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPooled;

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

        RedisEmbeddingStore store = RedisEmbeddingStore.builder()
                .host(ExternalValidationEnvironment.requireRedisHost())
                .port(ExternalValidationEnvironment.requireRedisPort())
                .indexName(names.indexName())
                .prefix(names.prefix())
                .dimension(8)
                .build();
        try {
            EmbeddingModel localModel = new DeterministicEightDimensionModel();
            KnowledgeSeedLoader loader = new KnowledgeSeedLoader(new KnowledgeSeedCatalog(), store, localModel);

            assertThat(loader.reload().documentsLoaded()).isPositive();
            assertThat(store.search(EmbeddingSearchRequest.builder()
                    .queryEmbedding(Embedding.from(new float[]{1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F}))
                    .maxResults(1)
                    .build()).matches()).isNotEmpty();
        } finally {
            ExternalValidationEnvironment.requireSafeRedisResources(names);
            try (JedisPooled redis = new JedisPooled(
                    ExternalValidationEnvironment.requireRedisHost(),
                    ExternalValidationEnvironment.requireRedisPort())) {
                redis.ftDropIndexDD(names.indexName());
            } finally {
                store.close();
            }
        }
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
