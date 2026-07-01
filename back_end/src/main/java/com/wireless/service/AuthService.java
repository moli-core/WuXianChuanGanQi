package com.wireless.service;

import com.wireless.model.vo.ApiResult;
import com.wireless.model.dto.LoginRequest;
import com.wireless.model.dto.RegisterRequest;

public interface AuthService {
    ApiResult<?> login(LoginRequest request);
    ApiResult<?> register(RegisterRequest request);
    ApiResult<?> profile(Long userId);
}
