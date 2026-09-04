package com.Liang.java.ai.langchain4j.knowledge;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class KnowledgeSeedCatalog {

    public static final String DOCUMENT_ID = "documentId";
    public static final String DEPARTMENT = "department";
    public static final String KNOWLEDGE_VERSION = "knowledgeVersion";

    private static final List<KnowledgeSeed> SEEDS = List.of(
            new KnowledgeSeed("hospital-overview", "src/main/resources/knowledge/医院信息.md", "", "2026.09"),
            new KnowledgeSeed("department-overview", "src/main/resources/knowledge/科室信息.md", "", "2026.09"),
            new KnowledgeSeed("neurology-overview", "src/main/resources/knowledge/神经内科.md", "神经内科", "2026.09"),
            new KnowledgeSeed("dentistry-overview", "src/main/resources/knowledge/口腔科.md", "口腔科", "2026.09"));

    public List<Document> loadDocuments() {
        return SEEDS.stream().map(this::load).toList();
    }

    private Document load(KnowledgeSeed seed) {
        Document source = FileSystemDocumentLoader.loadDocument(Path.of(seed.resourcePath()));
        Metadata metadata = source.metadata().copy()
                .put(DOCUMENT_ID, seed.documentId())
                .put(DEPARTMENT, seed.department())
                .put(KNOWLEDGE_VERSION, seed.knowledgeVersion());
        return Document.from(source.text(), metadata);
    }
}
