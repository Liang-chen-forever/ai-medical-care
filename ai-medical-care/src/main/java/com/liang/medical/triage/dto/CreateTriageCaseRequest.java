package com.liang.medical.triage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = CreateTriageCaseRequestDeserializer.class)
public record CreateTriageCaseRequest(
        @NotBlank(message = "主诉不能为空")
        @Size(min = 2, max = 1000, message = "主诉长度需为2到1000个字符")
        String chiefComplaint) {
    public CreateTriageCaseRequest {
        chiefComplaint = chiefComplaint == null ? null : chiefComplaint.trim();
    }
}
