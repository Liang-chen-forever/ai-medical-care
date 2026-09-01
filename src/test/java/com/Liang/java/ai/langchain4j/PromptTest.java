package com.Liang.java.ai.langchain4j;

import com.Liang.java.ai.langchain4j.assistant.MemoryChatAssistant;
import com.Liang.java.ai.langchain4j.assistant.SeparateChatAssistant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("external")
public class PromptTest {

    @Autowired
    private SeparateChatAssistant separateChatAssistant;

    @Test
    public void testSystemMessage(){
        String answer1 = separateChatAssistant.chat(4,"今天几号");
        System.out.println(answer1);
    }


    @Autowired
    private MemoryChatAssistant memoryChatAssistant;
    @Test
    public void userMessage(){
        String answer1 = memoryChatAssistant.chat("我是彭于晏");
        System.out.println(answer1);
        String answer2 = memoryChatAssistant.chat("我18了");
        System.out.println(answer2);
        String answer3 = memoryChatAssistant.chat("你知道我是谁吗？");
        System.out.println(answer3);
    }

    @Test
    public void testUserInfo(){

        //从数据库中获取用户信息(未完成)
        String username = "彭于晏";
        int age = 18;

        String answer1 = separateChatAssistant.chat3(20,"我是谁，现在几岁",username ,age);
        System.out.println(answer1);
    }
}
