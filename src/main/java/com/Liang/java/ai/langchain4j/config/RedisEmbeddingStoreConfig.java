package com.Liang.java.ai.langchain4j.config;

import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("redis")
public class RedisEmbeddingStoreConfig {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return RedisEmbeddingStore.builder()
                .host("localhost")
                .port(6379)
                .indexName("xiaozhi-index")
                .prefix("langchain4j:vector:xiaozhi:")
                .dimension(1024)
                .build();
    }
}