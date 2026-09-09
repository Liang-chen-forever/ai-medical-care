package com.liang.medical.service;

import com.liang.medical.auth.service.UserService;
import com.liang.medical.common.BusinessException;
import com.liang.medical.auth.UserRole;
import com.liang.medical.auth.dto.LoginRequest;
import com.liang.medical.auth.dto.RegisterRequest;
import com.liang.medical.auth.entity.User;
import com.liang.medical.auth.mapper.UserMapper;
import com.liang.medical.auth.service.UserServiceImpl;
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
    void registrationAlwaysAssignsPatientRole() {
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        userService.register(new RegisterRequest("alice", "plain-text", VALID_ID_CARD, "13800000000"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(UserRole.PATIENT);
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
