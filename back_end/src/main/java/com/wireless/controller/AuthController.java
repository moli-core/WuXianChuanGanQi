package com.wireless.controller;

import com.wireless.model.dto.LoginRequest;
import com.wireless.model.dto.RegisterRequest;
import com.wireless.model.vo.ApiResult;
import com.wireless.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户认证", description = "登录、注册、个人信息")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ApiResult<?> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public ApiResult<?> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/profile")
    public ApiResult<?> profile(@RequestAttribute(required = false) Long userId) {
        if (userId == null) {
            return ApiResult.error(401, "未登录");
        }
        return authService.profile(userId);
    }
}
