package com.Liang.java.ai.langchain4j;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class LLMTest {


    @Test
    public void TestGPTDemo() {

        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.openai.com/v1")
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .build();

        String response = model.chat("你好");
        System.out.println(response);
    }


    @Autowired
    private OpenAiChatModel model;

    @Test
    public void testSpringBoot(){
        String response = model.chat("你好");
        System.out.println(response);
    }
}
