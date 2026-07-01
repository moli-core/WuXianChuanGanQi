package com.wireless.service;

import com.wireless.model.vo.ApiResult;
import com.wireless.model.dto.LoginRequest;
import com.wireless.model.dto.RegisterRequest;

import java.util.Map;

public interface AuthService {
    ApiResult<?> login(LoginRequest request);
    ApiResult<?> register(RegisterRequest request);
    ApiResult<?> profile(Long userId);
    ApiResult<?> updateProfile(Long userId, String nickname, String phone, String email);
    ApiResult<?> changePassword(Long userId, String oldPassword, String newPassword);
}
