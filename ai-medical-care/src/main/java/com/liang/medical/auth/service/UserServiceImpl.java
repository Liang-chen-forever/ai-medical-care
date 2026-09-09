package com.liang.medical.auth.service;

import com.liang.medical.common.BusinessException;
import com.liang.medical.auth.UserRole;
import com.liang.medical.auth.dto.LoginRequest;
import com.liang.medical.auth.dto.RegisterRequest;
import com.liang.medical.auth.entity.User;
import com.liang.medical.auth.mapper.UserMapper;
import com.liang.medical.auth.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.baseMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User authenticate(LoginRequest request) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, request.username());
        User user = baseMapper.selectOne(queryWrapper);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, 401, "用户名或密码错误");
        }
        return user;
    }

    @Override
    public User register(RegisterRequest request) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, request.username());
        if (baseMapper.selectOne(queryWrapper) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "用户名或身份证号已被注册");
        }
        queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getIdCard, request.idCard());
        if (baseMapper.selectOne(queryWrapper) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "用户名或身份证号已被注册");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setIdCard(request.idCard());
        user.setPhone(request.phone());
        user.setRole(UserRole.PATIENT);
        user.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        baseMapper.insert(user);
        return user;
    }
}
