package com.Liang.java.ai.langchain4j.entity;

import com.Liang.java.ai.langchain4j.auth.UserRole;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String idCard;

    private String phone;

    private UserRole role;

    private String createTime;
}
