package com.Liang.java.ai.langchain4j.service;

import com.Liang.java.ai.langchain4j.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {

    User login(String username, String password);

    boolean register(User user);
}