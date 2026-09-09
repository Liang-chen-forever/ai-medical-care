package com.liang.medical.knowledge.controller;

import com.liang.medical.auth.LoginUser;
import com.liang.medical.auth.RequireRole;
import com.liang.medical.auth.UserPrincipal;
import com.liang.medical.auth.UserRole;
import com.liang.medical.common.ApiResponse;
import com.liang.medical.knowledge.dto.KnowledgeDocumentResponse;
import com.liang.medical.knowledge.entity.KnowledgeDocument;
import com.liang.medical.knowledge.service.KnowledgeDocumentService;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/knowledge/documents")
@RequireRole(UserRole.ADMIN)
public class KnowledgeDocumentController {
    private final KnowledgeDocumentService documentService;

    public KnowledgeDocumentController(KnowledgeDocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public ApiResponse<List<KnowledgeDocumentResponse>> list() {
        return ApiResponse.success(documentService.listVersions().stream()
                .map(KnowledgeDocumentResponse::from).toList());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<KnowledgeDocumentResponse> upload(@LoginUser UserPrincipal principal,
                                                           @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(KnowledgeDocumentResponse.from(documentService.upload(principal.userId(), file)));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<KnowledgeDocumentResponse> publish(@LoginUser UserPrincipal principal,
                                                           @PathVariable @Positive(message = "文档ID必须大于0") Long id) {
        return ApiResponse.success(KnowledgeDocumentResponse.from(documentService.publish(principal.userId(), id)));
    }

    @PostMapping("/{id}/rollback")
    public ApiResponse<KnowledgeDocumentResponse> rollback(@LoginUser UserPrincipal principal,
                                                            @PathVariable @Positive(message = "文档ID必须大于0") Long id) {
        return ApiResponse.success(KnowledgeDocumentResponse.from(documentService.rollback(principal.userId(), id)));
    }
}
