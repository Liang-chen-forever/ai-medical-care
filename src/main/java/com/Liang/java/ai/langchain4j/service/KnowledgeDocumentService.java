package com.Liang.java.ai.langchain4j.service;

import com.Liang.java.ai.langchain4j.entity.KnowledgeDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeDocumentService {
    KnowledgeDocument upload(Long adminId, MultipartFile file);

    List<KnowledgeDocument> listVersions();

    KnowledgeDocument publish(Long adminId, Long documentId);

    KnowledgeDocument rollback(Long adminId, Long documentId);
}
