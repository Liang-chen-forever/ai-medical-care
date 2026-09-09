package com.liang.medical;

import com.liang.medical.assistant.SeparateChatAssistant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("external")
public class ToolsTest {

    @Autowired
    private SeparateChatAssistant separateChatAssistant;

    @Test
    public void testCalculateTools(){
        String result = separateChatAssistant.chat(1, "1+1");
        System.out.println(result);
    }
}
