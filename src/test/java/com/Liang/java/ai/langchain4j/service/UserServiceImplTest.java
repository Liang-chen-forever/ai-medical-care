package com.Liang.java.ai.langchain4j.service;

import com.Liang.java.ai.langchain4j.common.BusinessException;
import com.Liang.java.ai.langchain4j.dto.auth.LoginRequest;
import com.Liang.java.ai.langchain4j.dto.auth.RegisterRequest;
import com.Liang.java.ai.langchain4j.entity.User;
import com.Liang.java.ai.langchain4j.mapper.UserMapper;
import com.Liang.java.ai.langchain4j.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private static final String VALID_ID_CARD = "11010519491231002X";

    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserServiceImpl(userMapper, passwordEncoder);
    }

    @Test
    void registerHashesPasswordBeforeSaving() {
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        userService.register(new RegisterRequest("alice", "plain-text", VALID_ID_CARD, "13800000000"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(saved.capture());
        assertThat(saved.getValue().getPassword()).isNotEqualTo("plain-text");
        assertThat(passwordEncoder.matches("plain-text", saved.getValue().getPassword())).isTrue();
    }

    @Test
    void authenticateRejectsIncorrectPassword() {
        User user = new User();
        user.setUsername("alice");
        user.setPassword(passwordEncoder.encode("correct-password"));
        when(userMapper.selectOne(any())).thenReturn(user);

        assertThatThrownBy(() -> userService.authenticate(new LoginRequest("alice", "wrong-password")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误");
    }
}
