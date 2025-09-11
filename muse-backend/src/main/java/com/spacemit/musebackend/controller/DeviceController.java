package com.spacemit.musebackend.controller;

import com.spacemit.musebackend.dto.*;
import com.spacemit.musebackend.service.AuthService;
import com.spacemit.musebackend.service.DeviceService;
import com.spacemit.musebackend.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/device")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DeviceController {

    private final DeviceService deviceService;
    private final AuthService authService;

    @PostMapping("/heartbeat")
    public ApiResponse<HeartbeatResponse> heartbeat(
            @Valid @RequestBody HeartbeatRequest request) {
        try {
            // 获取Token（优先从Authorization Header，其次从URL参数）
            String token = TokenUtil.getToken();
            if (!TokenUtil.isValidToken(token)) {
                log.warn("Token无效或未提供，来源: {}", TokenUtil.getTokenSource());
                return ApiResponse.error(401, "Token无效或未提供");
            }

            // 清理Token并验证
            String cleanToken = TokenUtil.cleanToken(token);
            if (!validateToken(cleanToken)) {
                log.warn("Token验证失败，来源: {}", TokenUtil.getTokenSource());
                return ApiResponse.error(401, "Token无效");
            }

            // 从token中获取deviceId，而不是从请求体中获取
            String deviceId = authService.getDeviceIdFromToken(cleanToken);
            
            // 设置deviceId到请求对象中
            request.setDeviceId(deviceId);
            
            log.info("心跳请求: deviceId={}, Token来源: {}", deviceId, TokenUtil.getTokenSource());
            return deviceService.processHeartbeat(request);
        } catch (Exception e) {
            log.error("心跳处理失败: {}", e.getMessage(), e);
            return ApiResponse.error("心跳处理失败: " + e.getMessage());
        }
    }

    @GetMapping("/online")
    public ApiResponse<String> deviceOnline() {
        try {
            // 获取Token（优先从Authorization Header，其次从URL参数）
            String token = TokenUtil.getToken();
            if (!TokenUtil.isValidToken(token)) {
                log.warn("Token无效或未提供，来源: {}", TokenUtil.getTokenSource());
                return ApiResponse.error(401, "Token无效或未提供");
            }

            log.info("设备上线请求，Token来源: {}", TokenUtil.getTokenSource());
            
            // 清理Token并验证
            String cleanToken = TokenUtil.cleanToken(token);
            if (!validateToken(cleanToken)) {
                log.warn("Token验证失败，来源: {}", TokenUtil.getTokenSource());
                return ApiResponse.error(401, "Token无效");
            }
            
            String deviceId = authService.getDeviceIdFromToken(cleanToken);
            log.info("设备上线通知: deviceId={}, Token来源: {}", deviceId, TokenUtil.getTokenSource());

            // 更新设备状态为在线
            return deviceService.setDeviceOnline(deviceId);
        } catch (Exception e) {
            log.error("设备上线通知失败: {}", e.getMessage(), e);
            return ApiResponse.error("设备上线通知失败: " + e.getMessage());
        }
    }

    @GetMapping("/offline")
    public ApiResponse<String> deviceOffline() {
        try {
            // 获取Token（优先从Authorization Header，其次从URL参数）
            String token = TokenUtil.getToken();
            if (!TokenUtil.isValidToken(token)) {
                log.warn("Token无效或未提供，来源: {}", TokenUtil.getTokenSource());
                return ApiResponse.error(401, "Token无效或未提供");
            }

            log.info("设备离线请求，Token来源: {}", TokenUtil.getTokenSource());
            
            // 清理Token并验证
            String cleanToken = TokenUtil.cleanToken(token);
            if (!validateToken(cleanToken)) {
                log.warn("Token验证失败，来源: {}", TokenUtil.getTokenSource());
                return ApiResponse.error(401, "Token无效");
            }
            
            String deviceId = authService.getDeviceIdFromToken(cleanToken);
            log.info("设备离线通知: deviceId={}, Token来源: {}", deviceId, TokenUtil.getTokenSource());

            // 更新设备状态为离线
            return deviceService.setDeviceOffline(deviceId);
        } catch (Exception e) {
            log.error("设备离线通知失败: {}", e.getMessage(), e);
            return ApiResponse.error("设备离线通知失败: " + e.getMessage());
        }
    }

    private boolean validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                log.warn("Token为空");
                return false;
            }
            
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            log.debug("验证Token: {}", token.substring(0, Math.min(20, token.length())) + "...");
            return authService.validateToken(token);
        } catch (Exception e) {
            log.error("Token验证异常: {}", e.getMessage(), e);
            return false;
        }
    }
}