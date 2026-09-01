package com.Liang.java.ai.langchain4j.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    private static final String SECRET = "01234567890123456789012345678901";

    @Test
    void createsAndParsesTokenForTheSameUser() {
        JwtTokenService tokenService = new JwtTokenService(SECRET, 3600);

        String token = tokenService.createToken(new UserPrincipal(7L, "alice"));

        assertThat(tokenService.parseToken(token)).isEqualTo(new UserPrincipal(7L, "alice"));
    }
}
