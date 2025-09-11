package com.spacemit.musebackend.controller;

import com.spacemit.musebackend.dto.ApiResponse;
import com.spacemit.musebackend.dto.LoginRequest;
import com.spacemit.musebackend.dto.LoginResponse;
import com.spacemit.musebackend.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("设备登录请求: deviceId={}, deviceType={}", request.getDeviceId(), request.getDeviceType());
            LoginResponse response = authService.login(request);
            return ApiResponse.success("登录成功", response);
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage(), e);
            return ApiResponse.error("登录失败: " + e.getMessage());
        }
    }

    @PostMapping("/validate")
    public ApiResponse<Object> validateToken(@RequestHeader("Authorization") String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            boolean isValid = authService.validateToken(token);
            
            if (isValid) {
                // 开发阶段返回deviceId信息
                String deviceId = authService.getDeviceIdFromToken(token);
                java.util.Map<String, Object> result = new java.util.HashMap<>();
                result.put("valid", true);
                result.put("deviceId", deviceId);
                return ApiResponse.success("Token验证完成", result);
            } else {
                return ApiResponse.success("Token验证完成", false);
            }
        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage(), e);
            return ApiResponse.error("Token验证失败: " + e.getMessage());
        }
    }
}