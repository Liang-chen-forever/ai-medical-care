package com.Liang.java.ai.langchain4j.knowledge;

import com.Liang.java.ai.langchain4j.dto.knowledge.KnowledgeReloadResponse;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.Liang.java.ai.langchain4j.mapper.KnowledgeDocumentMapper;
import com.Liang.java.ai.langchain4j.entity.KnowledgeDocument;

import java.util.List;
import java.util.ArrayList;

@Component
public class KnowledgeSeedLoader {

    private final KnowledgeSeedCatalog catalog;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final KnowledgeDocumentMapper documentMapper;

    public KnowledgeSeedLoader(KnowledgeSeedCatalog catalog, EmbeddingStore<TextSegment> embeddingStore,
                               EmbeddingModel embeddingModel) {
        this(catalog, embeddingStore, embeddingModel, null);
    }

    @Autowired
    public KnowledgeSeedLoader(KnowledgeSeedCatalog catalog, EmbeddingStore<TextSegment> embeddingStore,
                               EmbeddingModel embeddingModel, KnowledgeDocumentMapper documentMapper) {
        this.catalog = catalog;
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.documentMapper = documentMapper;
    }

    public KnowledgeReloadResponse reload() {
        List<Document> documents = new ArrayList<>(catalog.loadDocuments());
        if (documentMapper != null) {
            documents.addAll(documentMapper.findAllPublished().stream()
                    .filter(this::hasContent)
                    .map(this::toManagedDocument)
                    .toList());
        }
        embeddingStore.removeAll();
        EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build()
                .ingest(documents);
        return new KnowledgeReloadResponse(documents.size(), catalog.currentVersion());
    }

    private boolean hasContent(KnowledgeDocument document) {
        return document != null && document.getContentText() != null && !document.getContentText().isBlank();
    }

    private Document toManagedDocument(KnowledgeDocument document) {
        Metadata metadata = new Metadata()
                .put(KnowledgeSeedCatalog.DOCUMENT_ID,
                        "managed-" + document.getDocumentKey() + "-v" + document.getVersionNo())
                .put(KnowledgeSeedCatalog.DEPARTMENT, "")
                .put(KnowledgeSeedCatalog.KNOWLEDGE_VERSION, "managed-" + document.getVersionNo());
        return Document.from(document.getContentText(), metadata);
    }
}
