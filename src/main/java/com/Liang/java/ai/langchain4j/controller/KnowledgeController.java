package com.Liang.java.ai.langchain4j.controller;

import com.Liang.java.ai.langchain4j.auth.RequireRole;
import com.Liang.java.ai.langchain4j.auth.UserRole;
import com.Liang.java.ai.langchain4j.common.ApiResponse;
import com.Liang.java.ai.langchain4j.dto.knowledge.KnowledgeReloadResponse;
import com.Liang.java.ai.langchain4j.knowledge.KnowledgeSeedLoader;
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
