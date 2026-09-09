package com.liang.medical.config;


import com.liang.medical.store.MongoChatMemoryStore;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeparateChatAssistantConfig {

    @Autowired
    private MongoChatMemoryStore mongoChatMemoryStore;

    @Bean

    public ChatMemoryProvider chatMemoryProvider(){
        //实现自动注入memoryId
        return memoryId -> MessageWindowChatMemory
                .builder()
                .id(memoryId)
                .maxMessages(10)
                .chatMemoryStore(mongoChatMemoryStore)  //配置聊天记忆持久化
                //.chatMemoryStore(new InMemoryChatMemoryStore()) //都是存储在内容当中，不能持久化记忆
                .build();
    }
}
