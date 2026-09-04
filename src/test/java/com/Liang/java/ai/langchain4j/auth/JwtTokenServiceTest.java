package com.Liang.java.ai.langchain4j.auth;

import org.junit.jupiter.api.Test;
import com.Liang.java.ai.langchain4j.common.BusinessException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    private static final String SECRET = "01234567890123456789012345678901";

    @Test
    void tokenRoundTripRetainsRole() {
        JwtTokenService tokenService = new JwtTokenService(SECRET, 3600);
        UserPrincipal source = new UserPrincipal(7L, "doctor1", UserRole.DOCTOR);

        String token = tokenService.createToken(source);

        assertThat(tokenService.parseToken(token)).isEqualTo(source);
    }

    @Test
    void rejectsTokenWithoutRoleClaim() {
        JwtTokenService tokenService = new JwtTokenService(SECRET, 3600);
        String token = Jwts.builder()
                .subject("7")
                .claim("username", "alice")
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertInvalidRoleToken(tokenService, token);
    }

    @Test
    void rejectsTokenWithInvalidRoleClaim() {
        JwtTokenService tokenService = new JwtTokenService(SECRET, 3600);
        String token = Jwts.builder()
                .subject("7")
                .claim("username", "alice")
                .claim("role", "EDITOR")
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertInvalidRoleToken(tokenService, token);
    }

    private void assertInvalidRoleToken(JwtTokenService tokenService, String token) {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> tokenService.parseToken(token))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getCode()).isEqualTo(401);
                    assertThat(exception).hasMessage("登录状态无效或已过期");
                });
    }
}
