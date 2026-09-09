package com.liang.medical.controller;

import com.liang.medical.auth.RequireRole;
import com.liang.medical.auth.UserRole;
import com.liang.medical.common.ApiResponse;
import com.liang.medical.dto.knowledge.KnowledgeReloadResponse;
import com.liang.medical.knowledge.KnowledgeSeedLoader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "知识库管理")
@RestController
@RequestMapping("/api/v1/admin/knowledge")
@RequireRole(UserRole.ADMIN)
public class KnowledgeController {

    private final KnowledgeSeedLoader knowledgeSeedLoader;

    public KnowledgeController(KnowledgeSeedLoader knowledgeSeedLoader) {
        this.knowledgeSeedLoader = knowledgeSeedLoader;
    }

    @Operation(summary = "重新加载知识库到向量存储")
    @PostMapping("/reload")
    public ApiResponse<KnowledgeReloadResponse> reloadKnowledge() {
        return ApiResponse.success(knowledgeSeedLoader.reload());
    }
}
