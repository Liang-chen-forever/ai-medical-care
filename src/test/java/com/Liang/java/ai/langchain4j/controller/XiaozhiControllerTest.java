package com.Liang.java.ai.langchain4j.controller;

import com.Liang.java.ai.langchain4j.assistant.XiaozhiAgent;
import com.Liang.java.ai.langchain4j.auth.JwtTokenService;
import com.Liang.java.ai.langchain4j.auth.LoginRequiredInterceptor;
import com.Liang.java.ai.langchain4j.auth.LoginUserArgumentResolver;
import com.Liang.java.ai.langchain4j.auth.UserPrincipal;
import com.Liang.java.ai.langchain4j.common.GlobalExceptionHandler;
import com.Liang.java.ai.langchain4j.config.WebMvcConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = XiaozhiController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, LoginRequiredInterceptor.class,
        LoginUserArgumentResolver.class, XiaozhiControllerTest.JwtTestConfig.class})
class XiaozhiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private XiaozhiAgent xiaozhiAgent;

    @Test
    void anonymousChatRequestReturnsJsonUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/chat/conversations/9/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userMessage\":\"hello\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    void chatUsesUserScopedConversationMemoryKey() throws Exception {
        when(xiaozhiAgent.chat(eq("7:9"), eq("hello"), anyString())).thenReturn(Flux.just("response"));

        mockMvc.perform(post("/api/v1/chat/conversations/9/messages")
                        .header(HttpHeaders.AUTHORIZATION, bearerForUser7())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userMessage\":\"hello\"}"))
                .andExpect(status().isOk());

        verify(xiaozhiAgent).chat(eq("7:9"), eq("hello"), anyString());
    }

    private String bearerForUser7() {
        return "Bearer " + jwtTokenService.createToken(new UserPrincipal(7L, "alice"));
    }

    @TestConfiguration
    static class JwtTestConfig {

        @Bean
        JwtTokenService jwtTokenService() {
            return new JwtTokenService("01234567890123456789012345678901", 3600);
        }
    }
}
