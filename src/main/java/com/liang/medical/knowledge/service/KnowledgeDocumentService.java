package com.liang.medical.knowledge.service;

import com.liang.medical.knowledge.entity.KnowledgeDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeDocumentService {
    KnowledgeDocument upload(Long adminId, MultipartFile file);

    List<KnowledgeDocument> listVersions();

    KnowledgeDocument publish(Long adminId, Long documentId);

    KnowledgeDocument rollback(Long adminId, Long documentId);
}
