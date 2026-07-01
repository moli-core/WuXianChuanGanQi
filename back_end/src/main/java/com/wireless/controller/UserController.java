package com.wireless.controller;

import com.wireless.mapper.UserMapper;
import com.wireless.model.entity.User;
import com.wireless.model.vo.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理接口 (拓展)
 */
@Tag(name = "用户管理", description = "登录、权限分配 (拓展功能)")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;

    @Operation(summary = "根据 ID 查询用户信息")
    @GetMapping("/{id}")
    public ApiResult<User> getUser(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return ApiResult.error("用户不存在");
        }
        user.setPassword(null); // 不返回密码
        return ApiResult.success(user);
    }

    @Operation(summary = "健康检查")
    @GetMapping("/ping")
    public ApiResult<String> ping() {
        return ApiResult.success("pong");
    }
}
