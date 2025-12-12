package com.spacemit.musebackendv2.controller;

import com.spacemit.musebackendv2.dto.ApiResponse;
import com.spacemit.musebackendv2.dto.TokenResponse;
import com.spacemit.musebackendv2.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;

/**
 * 认证控制器
 * 接口路径: /api/v2/auth
 */
@RestController
@RequestMapping("/api/v2/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostConstruct
    public void init() {
        log.info("✅✅✅ AuthController Bean已创建，路径: /api/v2/auth");
        System.out.println("✅✅✅ AuthController Bean已创建，路径: /api/v2/auth");
    }

    /**
     * Token获取接口
     * POST /api/v2/auth/token
     * 
     * 请求参数: deviceId (URL参数或表单参数)
     * 响应: TokenResponse
     */
    @PostMapping("/token")
    public ApiResponse<TokenResponse> getToken(@RequestParam("deviceId") String deviceId) {
        try {
            log.info("Token获取请求: deviceId={}", deviceId);
            TokenResponse response = authService.getTokenForDevice(deviceId);
            return ApiResponse.success("Token获取成功", response);
        } catch (Exception e) {
            log.error("Token获取失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
            return ApiResponse.error(500, "Token获取失败: " + e.getMessage());
        }
    }
}
















