package com.Liang.java.ai.langchain4j;

import com.Liang.java.ai.langchain4j.assistant.SeparateChatAssistant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ToolsTest {

    @Autowired
    private SeparateChatAssistant separateChatAssistant;

    @Test
    public void testCalculateTools(){
        String result = separateChatAssistant.chat(1, "1+1");
        System.out.println(result);
    }
}
