package com.liang.medical.service;

import com.liang.medical.dto.auth.LoginRequest;
import com.liang.medical.dto.auth.RegisterRequest;
import com.liang.medical.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {

    User authenticate(LoginRequest request);

    User register(RegisterRequest request);
}
