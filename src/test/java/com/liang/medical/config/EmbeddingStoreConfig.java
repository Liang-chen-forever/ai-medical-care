package com.liang.medical.config;


import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeServerlessIndexConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("pinecone")
public class EmbeddingStoreConfig {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {

        //创建向量存储
        EmbeddingStore<TextSegment> embeddingStore = PineconeEmbeddingStore.builder()
                .apiKey("pcsk_29FXHN_7Z7yHmj6RWVWyksGGxk3ru4EAAdR8snYFTg6k6N9MyindX72uBCzN73bR6dYLru")
                .index("xiaozhi-index")   //如果指定的索引不存在，将创建一个新索引
                .nameSpace("xiaozhi-namespace")  //如果指定的命名空间不存在，将创建一个新命名空间
                .createIndex(PineconeServerlessIndexConfig.builder()
                        .cloud("AWS")   //指定索引部署在AWS云服务上
                        .region("us-east-1")  //指定索引部署在AWS的us-east-1区域
                        .dimension(embeddingModel.dimension())   //指定索引的向量维度，该维度与embeddingModel生成的向量维度相同。
                        .build())
                .build();

        return embeddingStore;
    }
}