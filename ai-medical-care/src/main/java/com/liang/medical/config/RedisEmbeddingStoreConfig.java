package com.liang.medical.config;

import com.liang.medical.knowledge.KnowledgeSeedCatalog;
import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@Profile("redis")
@EnableConfigurationProperties(KnowledgeRedisProperties.class)
public class RedisEmbeddingStoreConfig {

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(KnowledgeRedisProperties properties) {
        return RedisEmbeddingStore.builder()
                .host(properties.getHost())
                .port(properties.getPort())
                .indexName(properties.getIndexName())
                .prefix(properties.getPrefix())
                .dimension(properties.getDimension())
                .metadataKeys(KnowledgeSeedCatalog.metadataKeys())
                .build();
    }
}
