package com.Liang.java.ai.langchain4j.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度必须在3到50个字符之间") String username,
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 72, message = "密码长度必须在6到72个字符之间") String password,
        @NotBlank(message = "身份证号不能为空")
        @Pattern(regexp = "^[0-9]{17}[0-9Xx]$", message = "身份证号格式不正确") String idCard,
        @Size(max = 20, message = "手机号长度不能超过20个字符") String phone
) {
}
