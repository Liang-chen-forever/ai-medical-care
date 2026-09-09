package com.liang.medical.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectAppointmentRequest(
        @NotBlank(message = "拒绝原因不能为空")
        @Size(max = 200, message = "拒绝原因不能超过200个字符") String reason) {
}
