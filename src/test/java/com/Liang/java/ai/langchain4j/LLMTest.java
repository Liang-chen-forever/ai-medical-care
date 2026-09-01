package com.Liang.java.ai.langchain4j;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.WanxImageModel;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import net.bytebuddy.agent.VirtualMachine;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.awt.*;
import java.net.URI;

@SpringBootTest
@Tag("external")
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

    @Autowired
    private OllamaChatModel ollamaModel;

    @Test
    public void testOllama(){
        String response = ollamaModel.chat("你好");
        System.out.println(response);
    }

    @Autowired
    private QwenChatModel qwenModel;

    @Test
    public void testQwen(){
        String response = qwenModel.chat("你好");
        System.out.println(response);
    }


    // 测试DashScope Wanx模型---文生图模型
    @Test
    public void testDashScopeWanx(){
        WanxImageModel wanxImageModel = WanxImageModel
                .builder()
                .modelName("wanx2.1-t2i-turbo")
                .apiKey("${DASH_SCOPE_API_KEY}")
                .build();

        Response<Image> response = wanxImageModel.generate("");
        URI url = response.content().url();
        System.out.println(url);
    }
}
