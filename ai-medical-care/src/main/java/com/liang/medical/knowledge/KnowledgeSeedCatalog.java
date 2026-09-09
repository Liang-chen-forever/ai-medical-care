package com.liang.medical.knowledge;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.springframework.stereotype.Component;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.nio.file.Path;
import java.util.List;

@Component
public class KnowledgeSeedCatalog {

    public static final String DOCUMENT_ID = "documentId";
    public static final String DEPARTMENT = "department";
    public static final String KNOWLEDGE_VERSION = "knowledgeVersion";

    /** Metadata fields that must survive the vector-store round trip for traceable triage evidence. */
    public static List<String> metadataKeys() {
        return List.of(DOCUMENT_ID, DEPARTMENT, KNOWLEDGE_VERSION);
    }

    private static final List<KnowledgeSeed> SEEDS = List.of(
            new KnowledgeSeed("hospital-overview", "knowledge/医院信息.md", "", "2026.09"),
            new KnowledgeSeed("department-overview", "knowledge/科室信息.md", "", "2026.09"),
            new KnowledgeSeed("neurology-overview", "knowledge/神经内科.md", "神经内科", "2026.09"),
            new KnowledgeSeed("dentistry-overview", "knowledge/口腔科.md", "口腔科", "2026.09"));

    public List<Document> loadDocuments() {
        return SEEDS.stream().map(this::load).toList();
    }

    public String currentVersion() {
        return SEEDS.stream().map(KnowledgeSeed::knowledgeVersion).distinct().reduce((a, b) -> {
            throw new IllegalStateException("知识库版本不一致");
        }).orElseThrow(() -> new IllegalStateException("知识库版本缺失"));
    }

    private Document load(KnowledgeSeed seed) {
        String text;
        try (var stream = new ClassPathResource(seed.resourcePath()).getInputStream()) {
            text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("无法加载知识库资源: " + seed.resourcePath(), e);
        }
        Document source = Document.from(text);
        Metadata metadata = source.metadata().copy()
                .put(DOCUMENT_ID, seed.documentId())
                .put(DEPARTMENT, seed.department())
                .put(KNOWLEDGE_VERSION, seed.knowledgeVersion());
        return Document.from(source.text(), metadata);
    }
}
