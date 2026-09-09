package com.liang.medical.bean;


import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document("chat_messages")
public class ChatMessages {


    //唯一标识，映射到MongoDB的_id字段
    @Id
    private ObjectId messageId;

    private String memoryId;


    //存储当前聊天记录列表到json字符串
    private String content;
}
