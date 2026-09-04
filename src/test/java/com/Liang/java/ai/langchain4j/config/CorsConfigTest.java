package com.Liang.java.ai.langchain4j.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsConfigTest {

    @Test
    void allowsLocalWebOriginAndAuthenticationHeaderForPreflightLogin() throws Exception {
        CorsFilter filter = corsFilterFromApplicationDefaults();
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login");
        request.addHeader("Origin", "http://127.0.0.1:3000");
        request.addHeader("Access-Control-Request-Method", "POST");
        request.addHeader("Access-Control-Request-Headers", "content-type,authentication");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> assertFalse(true,
                "CORS preflight should be handled before reaching the application");

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals("http://127.0.0.1:3000", response.getHeader("Access-Control-Allow-Origin"));
        assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
        assertTrue(response.getHeader("Access-Control-Allow-Headers").contains("authentication"));
    }

    private CorsFilter corsFilterFromApplicationDefaults() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IOException("application.properties not found");
            }
            properties.load(input);
        }

        String configuredOrigins = properties.getProperty("app.cors.allowed-origins");
        String prefix = "${APP_CORS_ALLOWED_ORIGINS:";
        String defaults = configuredOrigins.substring(prefix.length(), configuredOrigins.length() - 1);
        return new CorsConfig(defaults).corsFilter();
    }
}
