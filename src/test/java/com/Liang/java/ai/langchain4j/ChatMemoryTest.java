package com.Liang.java.ai.langchain4j;


import com.Liang.java.ai.langchain4j.assistant.Assistant;
import com.Liang.java.ai.langchain4j.assistant.MemoryChatAssistant;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.spring.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ChatMemoryTest {

    @Autowired
    private Assistant assistant;

    @Test
    public void testChatMemory(){

        String answer1 = assistant.chat("我是GPT5.5");
        System.out.println(answer1);

        String answer2 = assistant.chat("你真的比我厉害吗？");
        System.out.println(answer2);
    }

    @Autowired
    private QwenChatModel qwenChatModel;

    @Test
    public void testChatMemory2(){

        //第一轮对话
        UserMessage userMessage1 = new UserMessage("我是GPT5.5");
        ChatRequest chatRequest1 = ChatRequest.builder()
                .messages(userMessage1)
                .build();
        ChatResponse chatResponse1 = qwenChatModel.chat(chatRequest1);
        AiMessage aiMessage1 = chatResponse1.aiMessage();
        System.out.println(aiMessage1.text());

        UserMessage userMessage2 = new UserMessage("我是谁");
        ChatRequest chatRequest2 = ChatRequest.builder()
                .messages(userMessage1, aiMessage1, userMessage2)
                .build();
        ChatResponse chatResponse2 = qwenChatModel.chat(chatRequest2);
        AiMessage aiMessage2 = chatResponse2.aiMessage();
        System.out.println(aiMessage2.text());

    }

    @Test

    public void testChatMemory3(){

        //通过AiService构建聊天记忆
        MessageWindowChatMemory messageWindowChatMemory = MessageWindowChatMemory.withMaxMessages(10);

        Assistant assistant = AiServices
                .builder(Assistant.class)
                .chatLanguageModel(qwenChatModel)   // 指定聊天语言模型
                .chatMemory(messageWindowChatMemory)  // 指定聊天记忆
                .build();

        String answer1 = assistant.chat("我是GPT5.5");
        System.out.println(answer1);
        String answer2 = assistant.chat("你真的比我厉害吗？");
        System.out.println(answer2);
    }

    @Autowired
    private MemoryChatAssistant memoryChatAssistant;
    @Test
    public void testChatMemory4(){


        String answer1 = memoryChatAssistant.chat("我是GPT5.5");
        System.out.println(answer1);
        String answer2 = memoryChatAssistant.chat("你真的比我厉害吗？");
        System.out.println(answer2);
    }
}
