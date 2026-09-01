package com.Liang.java.ai.langchain4j.assistant;


import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
        chatModel = "openAiChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools = "calculatorTools"  //配置工具
)
public interface SeparateChatAssistant {

    //@SystemMessage("你是我的好朋友，请用普通话回答问题,今天是{{current_date}}")  //系统消息提示词
    @SystemMessage(fromResource = "my-prompt-template.txt")
    String chat(@MemoryId int memoryId, @UserMessage  String userMessage);

    @UserMessage("你是我的好朋友，请用普通话回答问题.{{message}}")
    String chat2(@MemoryId int memoryId , @V("message") String userMessage);  //多个参数必须用V注解来确认指出，一个参数可以不用

    @SystemMessage(fromResource = "my-prompt-template3.txt")
    String chat3(
            @MemoryId int memoryId,
            @UserMessage String userMessage,
            @V("username") String username,
            @V("age") int age
    );
}