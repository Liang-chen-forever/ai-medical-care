package com.liang.medical.auth.service;

import com.liang.medical.auth.dto.LoginRequest;
import com.liang.medical.auth.dto.RegisterRequest;
import com.liang.medical.auth.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {

    User authenticate(LoginRequest request);

    User register(RegisterRequest request);
}
