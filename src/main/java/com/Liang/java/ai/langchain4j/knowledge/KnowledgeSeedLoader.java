package com.Liang.java.ai.langchain4j.knowledge;

import com.Liang.java.ai.langchain4j.dto.knowledge.KnowledgeReloadResponse;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeSeedLoader {

    private final KnowledgeSeedCatalog catalog;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public KnowledgeSeedLoader(KnowledgeSeedCatalog catalog, EmbeddingStore<TextSegment> embeddingStore) {
        this.catalog = catalog;
        this.embeddingStore = embeddingStore;
    }

    public KnowledgeReloadResponse reload() {
        List<Document> documents = catalog.loadDocuments();
        embeddingStore.removeAll();
        EmbeddingStoreIngestor.ingest(documents, embeddingStore);
        return new KnowledgeReloadResponse(documents.size(), catalog.currentVersion());
    }
}
