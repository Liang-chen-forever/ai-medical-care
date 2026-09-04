package com.Liang.java.ai.langchain4j.auth;

/**
 * 已通过令牌校验的请求身份。
 */
public record UserPrincipal(Long userId, String username, UserRole role) {
}
