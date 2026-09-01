package com.Liang.java.ai.langchain4j.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!redis & !pinecone")
public class DefaultEmbeddingStoreConfig {

    @Bean
    @ConditionalOnMissingBean(EmbeddingStore.class)
    public EmbeddingStore<TextSegment> defaultEmbeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }
}