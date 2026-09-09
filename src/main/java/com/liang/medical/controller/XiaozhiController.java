package com.liang.medical.controller;


import com.liang.medical.assistant.XiaozhiAgent;
import com.liang.medical.auth.LoginUser;
import com.liang.medical.auth.UserPrincipal;
import com.liang.medical.bean.ChatForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Tag(name = "硅谷小智")
@RestController
@RequestMapping("/api/v1/chat")
public class XiaozhiController {

    private final XiaozhiAgent xiaozhiAgent;

    public XiaozhiController(XiaozhiAgent xiaozhiAgent) {
        this.xiaozhiAgent = xiaozhiAgent;
    }

    @Operation(summary = "与小小智聊天")
    @PostMapping(value = "/conversations/{conversationId}/messages", produces = "text/stream;charset=utf-8")
    public Flux<String> chat(@LoginUser UserPrincipal user,
                             @PathVariable @Positive(message = "会话ID必须大于0") Long conversationId,
                             @Valid @RequestBody ChatForm chatForm) {
        String memoryId = user.userId() + ":" + conversationId;
        return xiaozhiAgent.chat(memoryId, chatForm.userMessage(), LocalDate.now().toString());
    }
}
