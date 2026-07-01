package com.wireless.config;

import com.wireless.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 身份认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    /** 不需要认证的路径 */
    private static final List<String> WHITELIST = Arrays.asList(
            "/v3/api-docs", "/swagger", "/actuator", "/ws/",
            "/api/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // 根路径 + health 放行
        if (path.equals("/") || path.equals("/favicon.ico") || path.equals("/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 白名单放行
        for (String white : WHITELIST) {
            if (path.startsWith(white)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        // 从 Header 中获取 Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validate(token)) {
                // 将用户信息存入 request attribute，controller 中可用
                request.setAttribute("userId", jwtUtil.getUserId(token));
                request.setAttribute("username", jwtUtil.getUsername(token));
                request.setAttribute("role", jwtUtil.getRole(token));
                filterChain.doFilter(request, response);
                return;
            }
        }

        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未登录或 Token 已过期\"}");
    }
}
