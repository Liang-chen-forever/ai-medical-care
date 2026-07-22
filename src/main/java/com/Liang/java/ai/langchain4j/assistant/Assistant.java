package com.Liang.java.ai.langchain4j.assistant;


import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(wiringMode = EXPLICIT,chatModel = "QwenChatModel")
public interface Assistant {
    String chat(String userMessage);
}
