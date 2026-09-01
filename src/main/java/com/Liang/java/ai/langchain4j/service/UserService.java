package com.Liang.java.ai.langchain4j.service;

import com.Liang.java.ai.langchain4j.dto.auth.LoginRequest;
import com.Liang.java.ai.langchain4j.dto.auth.RegisterRequest;
import com.Liang.java.ai.langchain4j.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {

    User authenticate(LoginRequest request);

    User register(RegisterRequest request);
}
