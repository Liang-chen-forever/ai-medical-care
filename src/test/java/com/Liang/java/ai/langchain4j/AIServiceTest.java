package com.Liang.java.ai.langchain4j;


import com.Liang.java.ai.langchain4j.assistant.Assistant;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.spring.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AIServiceTest {

    @Autowired

    private QwenChatModel qwenChatModel;

    @Test
    public void testChat(){
        Assistant assistant = AiServices.create(Assistant.class, qwenChatModel);
        String response = assistant.chat("你好");
        System.out.println(response);
    }

    @Autowired
    private Assistant assistant ;

    @Test
    public void testChat2(){
        String response = assistant.chat("你好");
        System.out.println(response);
    }
}
