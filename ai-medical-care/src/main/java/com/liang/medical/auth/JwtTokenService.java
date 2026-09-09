package com.liang.medical.auth;

import com.liang.medical.common.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.http.HttpStatus;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * 负责签发和解析系统访问令牌。
 */
public class JwtTokenService {

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtTokenService(String secret, long expirationMillis) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT 密钥至少需要 32 个字符");
        }
        if (expirationMillis <= 0) {
            throw new IllegalArgumentException("JWT 有效期必须大于 0");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    public String createToken(UserPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(principal.userId()))
                .claim("username", principal.username())
                .claim("role", principal.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(signingKey)
                .compact();
    }

    public UserPrincipal parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String role = claims.get("role", String.class);
            if (role == null) {
                throw new IllegalArgumentException("JWT role claim is required");
            }
            return new UserPrincipal(Long.valueOf(claims.getSubject()), claims.get("username", String.class),
                    UserRole.valueOf(role));
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, 401, "登录状态无效或已过期");
        }
    }
}
