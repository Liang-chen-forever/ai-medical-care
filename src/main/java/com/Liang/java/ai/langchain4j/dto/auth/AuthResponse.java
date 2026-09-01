package com.Liang.java.ai.langchain4j.dto.auth;

/**
 * 登录成功后返回的公开用户信息，不包含密码。
 */
public record AuthResponse(
        String accessToken,
        Long userId,
        String username,
        String idCard,
        String phone
) {
}
