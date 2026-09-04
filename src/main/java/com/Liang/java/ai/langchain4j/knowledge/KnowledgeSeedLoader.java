package com.Liang.java.ai.langchain4j.knowledge;

import com.Liang.java.ai.langchain4j.dto.knowledge.KnowledgeReloadResponse;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeSeedLoader {

    private final KnowledgeSeedCatalog catalog;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public KnowledgeSeedLoader(KnowledgeSeedCatalog catalog, EmbeddingStore<TextSegment> embeddingStore,
                               EmbeddingModel embeddingModel) {
        this.catalog = catalog;
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    public KnowledgeReloadResponse reload() {
        List<Document> documents = catalog.loadDocuments();
        embeddingStore.removeAll();
        EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build()
                .ingest(documents);
        return new KnowledgeReloadResponse(documents.size(), catalog.currentVersion());
    }
}
