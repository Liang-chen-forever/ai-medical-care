package com.Liang.java.ai.langchain4j.config;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 应用启动时自动加载知识库文档到向量存储
 */
@Component
public class KnowledgeBaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseInitializer.class);

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    private static final String KNOWLEDGE_DIR = "src/main/resources/knowledge";
    private static final String[] KNOWLEDGE_FILES = {
            "医院信息.md", "科室信息.md", "神经内科.md", "口腔科.md"
    };

    @Override
    public void run(String... args) {
        try {
            List<Document> documents = new ArrayList<>();
            for (String fileName : KNOWLEDGE_FILES) {
                String filePath = KNOWLEDGE_DIR + File.separator + fileName;
                File file = new File(filePath);
                if (file.exists()) {
                    Document doc = FileSystemDocumentLoader.loadDocument(file.toPath());
                    documents.add(doc);
                    log.info("已加载知识库文档: {}", fileName);
                } else {
                    log.warn("知识库文档不存在: {}", filePath);
                }
            }

            if (!documents.isEmpty()) {
                EmbeddingStoreIngestor.ingest(documents, embeddingStore);
                log.info("知识库初始化完成，共加载 {} 个文档到向量存储", documents.size());
            }
        } catch (Exception e) {
            log.error("知识库初始化失败: {}", e.getMessage(), e);
        }
    }
}