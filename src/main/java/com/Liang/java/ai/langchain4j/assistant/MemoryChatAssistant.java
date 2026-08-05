package com.Liang.java.ai.langchain4j.assistant;



import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

/*
 * 初级智能体
 */
@AiService(wiringMode = EXPLICIT,
           chatModel = "openAiChatModel",
           chatMemory = "chatMemory"
)
public interface MemoryChatAssistant {

    @UserMessage("你是我的好朋友，请用普通话回答问题，并且添加一些表情符号。{{Message}}")  //用户消息提示词
    String chat(@V("Message") String Message); //使用V注解，明确的指出这个参数是用户消息
}