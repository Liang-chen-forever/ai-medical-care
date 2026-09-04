package com.Liang.java.ai.langchain4j.dto.triage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTriageCaseRequest(
        @NotBlank(message = "主诉不能为空")
        @Size(min = 2, max = 1000, message = "主诉长度需为2到1000个字符")
        String chiefComplaint) {}
