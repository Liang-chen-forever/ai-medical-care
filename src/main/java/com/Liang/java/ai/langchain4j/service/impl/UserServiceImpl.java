package com.Liang.java.ai.langchain4j.service.impl;

import com.Liang.java.ai.langchain4j.entity.User;
import com.Liang.java.ai.langchain4j.mapper.UserMapper;
import com.Liang.java.ai.langchain4j.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User login(String username, String password) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        queryWrapper.eq(User::getPassword, password);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public boolean register(User user) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, user.getUsername());
        if (baseMapper.selectOne(queryWrapper) != null) {
            return false;
        }
        // 检查身份证号是否已注册
        queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getIdCard, user.getIdCard());
        if (baseMapper.selectOne(queryWrapper) != null) {
            return false;
        }
        user.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return save(user);
    }
}