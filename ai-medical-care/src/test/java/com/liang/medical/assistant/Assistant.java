package com.liang.medical.assistant;


import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
        chatModel = "openAiChatModel"
)
public interface Assistant {
    String chat(String userMessage);
}