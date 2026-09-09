package com.liang.medical;


import com.liang.medical.bean.ChatMessages;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@SpringBootTest(properties = "DASH_SCOPE_API_KEY=test-key")
@Tag("external")
public class MongoCrudTest {
    @Autowired
    private MongoTemplate mongoTemplate;


    /*
    * 测试MongoDB数据库
     */
/*    @Test
    public void testInsert1(){
        mongoTemplate.insert(new ChatMessages(1L,"聊天记录"));
    }*/

    @Test
    public void testInsert2(){
        ChatMessages chatMessages = new ChatMessages();
        chatMessages.setContent("聊天记录列表");
        mongoTemplate.insert(chatMessages);
    }

    @Test
    public void testFindById(){
        ChatMessages chatMessages = mongoTemplate.findById(1L, ChatMessages.class);
        System.out.println(chatMessages);
    }

    @Test
    public void testUpdate(){

        Criteria criteria = Criteria.where("_id").is(1L);
        Query query = new Query(criteria);
        Update update = new Update();
        update.set("content","更新聊天记录列表");

        mongoTemplate.upsert(query,update,ChatMessages.class);

    }

    @Test
    public void testDelete(){
        Criteria criteria = Criteria.where("_id").is(1L);
        Query query = new Query(criteria);
        mongoTemplate.remove(query,ChatMessages.class);
    }
}
