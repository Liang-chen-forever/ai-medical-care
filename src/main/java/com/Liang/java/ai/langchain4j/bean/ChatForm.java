package com.Liang.java.ai.langchain4j.bean;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatForm(
        @NotBlank(message = "消息不能为空")
        @Size(max = 1000, message = "消息不能超过1000个字符") String userMessage) {
}
