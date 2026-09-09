package com.liang.medical.auth;

/**
 * 已通过令牌校验的请求身份。
 */
public record UserPrincipal(Long userId, String username, UserRole role) {
}
