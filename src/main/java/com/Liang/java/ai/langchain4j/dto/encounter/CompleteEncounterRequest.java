package com.Liang.java.ai.langchain4j.dto.encounter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteEncounterRequest(
        @NotBlank(message = "就诊摘要不能为空")
        @Size(max = 2000, message = "就诊摘要不能超过2000个字符")
        String summary,
        @Size(max = 2000, message = "随访建议不能超过2000个字符")
        String followUpAdvice) {
}
