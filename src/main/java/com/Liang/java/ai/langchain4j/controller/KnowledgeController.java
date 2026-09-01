package com.Liang.java.ai.langchain4j.controller;

import com.Liang.java.ai.langchain4j.bean.Result;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Tag(name = "知识库管理")
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Operation(summary = "重新加载知识库到向量存储")
    @PostMapping("/reload")
    public Result<String> reloadKnowledge() {
        try {
            Document doc1 = FileSystemDocumentLoader.loadDocument("src/main/resources/knowledge/医院信息.md");
            Document doc2 = FileSystemDocumentLoader.loadDocument("src/main/resources/knowledge/科室信息.md");
            Document doc3 = FileSystemDocumentLoader.loadDocument("src/main/resources/knowledge/神经内科.md");
            Document doc4 = FileSystemDocumentLoader.loadDocument("src/main/resources/knowledge/口腔科.md");
            List<Document> documents = Arrays.asList(doc1, doc2, doc3, doc4);

            EmbeddingStoreIngestor.ingest(documents, embeddingStore);
            return Result.success("知识库重新加载成功，共加载 " + documents.size() + " 个文档");
        } catch (Exception e) {
            return Result.error("知识库加载失败: " + e.getMessage());
        }
    }
}