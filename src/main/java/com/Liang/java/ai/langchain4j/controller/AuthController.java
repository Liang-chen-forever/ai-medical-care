package com.Liang.java.ai.langchain4j.controller;

import com.Liang.java.ai.langchain4j.bean.Result;
import com.Liang.java.ai.langchain4j.entity.User;
import com.Liang.java.ai.langchain4j.service.UserService;
import com.Liang.java.ai.langchain4j.util.IdCardValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "用户认证")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody User user) {
        if (user.getUsername() == null || user.getPassword() == null) {
            return Result.error("用户名和密码不能为空");
        }
        User dbUser = userService.login(user.getUsername(), user.getPassword());
        if (dbUser == null) {
            return Result.error("用户名或密码错误");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", dbUser.getId());
        data.put("username", dbUser.getUsername());
        data.put("idCard", dbUser.getIdCard());
        data.put("phone", dbUser.getPhone());
        return Result.success("登录成功", data);
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<String> register(@RequestBody User user) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (user.getUsername().trim().length() < 3) {
            return Result.error("用户名至少需要3个字符");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return Result.error("密码不能为空");
        }
        if (user.getPassword().trim().length() < 6) {
            return Result.error("密码至少需要6位");
        }
        // 身份证号校验（格式 + 校验码算法）
        String idCardError = IdCardValidator.validate(user.getIdCard());
        if (idCardError != null) {
            return Result.error(idCardError);
        }
        boolean success = userService.register(user);
        if (!success) {
            return Result.error("用户名或身份证号已被注册");
        }
        return Result.success("注册成功");
    }
}