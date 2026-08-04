package com.Liang.java.ai.langchain4j;


import com.Liang.java.ai.langchain4j.bean.ChatMessages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

@SpringBootTest(properties = "DASH_SCOPE_API_KEY=test-key")
public class MongoCrudTest {
    @Autowired
    private MongoTemplate mongoTemplate;


    /*
    * 测试MongoDB数据库
     */
    @Test
    public void testInsert(){
        mongoTemplate.insert(new ChatMessages(1L,"聊天记录"));
    }
}