package com.Liang.java.ai.langchain4j.dto.knowledge;

import com.Liang.java.ai.langchain4j.entity.KnowledgeDocument;
import com.Liang.java.ai.langchain4j.knowledge.KnowledgeDocumentStatus;

import java.time.LocalDateTime;

public record KnowledgeDocumentResponse(Long id, String documentKey, String documentName,
                                        String contentType, String contentSha256, Integer versionNo,
                                        KnowledgeDocumentStatus status, Long createdBy,
                                        LocalDateTime createdAt, LocalDateTime publishedAt) {
    public static KnowledgeDocumentResponse from(KnowledgeDocument document) {
        return new KnowledgeDocumentResponse(document.getId(), document.getDocumentKey(), document.getDocumentName(),
                document.getContentType(), document.getContentSha256(), document.getVersionNo(), document.getStatus(),
                document.getCreatedBy(), document.getCreatedAt(), document.getPublishedAt());
    }
}
