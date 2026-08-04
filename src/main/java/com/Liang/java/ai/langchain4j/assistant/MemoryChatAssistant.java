package com.Liang.java.ai.langchain4j.assistant;



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

    String chat(String Message);
}