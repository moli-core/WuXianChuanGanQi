package com.wireless.service.impl;

import com.wireless.mapper.UserMapper;
import com.wireless.model.dto.LoginRequest;
import com.wireless.model.dto.RegisterRequest;
import com.wireless.model.entity.User;
import com.wireless.model.vo.ApiResult;
import com.wireless.service.AuthService;
import com.wireless.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public ApiResult<?> login(LoginRequest request) {
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            return ApiResult.error("用户不存在");
        }
        if (user.getStatus() == 0) {
            return ApiResult.error("账号已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ApiResult.error("密码错误");
        }

        userMapper.updateLastLogin(user.getId());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("role", user.getRole());
        return ApiResult.success(data);
    }

    @Override
    @Transactional
    public ApiResult<?> register(RegisterRequest request) {
        User exist = userMapper.selectByUsername(request.getUsername());
        if (exist != null) {
            return ApiResult.error("用户名已存在");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname() != null ? request.getNickname() : request.getUsername())
                .phone(request.getPhone())
                .email(request.getEmail())
                .role("user")
                .status(1)
                .build();
        userMapper.insert(user);

        log.info("新用户注册: {}", user.getUsername());
        return ApiResult.success("注册成功");
    }

    @Override
    public ApiResult<?> profile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return ApiResult.error("用户不存在");
        }
        user.setPassword(null);
        return ApiResult.success(user);
    }
}
