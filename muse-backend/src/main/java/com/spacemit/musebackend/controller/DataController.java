package com.spacemit.musebackend.controller;

import com.spacemit.musebackend.dto.*;
import com.spacemit.musebackend.service.AuthService;
import com.spacemit.musebackend.service.DataService;
import com.spacemit.musebackend.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/data")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DataController {

    private final DataService dataService;
    private final AuthService authService;

    @PostMapping("/realtime")
    public ApiResponse<DataAck> uploadRealtimeData(
            @Valid @RequestBody RealtimeDataRequest request) {
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
            
            log.info("实时数据上传: deviceId={}, Token来源: {}", deviceId, TokenUtil.getTokenSource());
            return dataService.processRealtimeData(request);
        } catch (Exception e) {
            log.error("实时数据上传失败: {}", e.getMessage(), e);
            return ApiResponse.error("实时数据上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/gps")
    public ApiResponse<DataAck> uploadGpsData(
            @Valid @RequestBody RealtimeGpsRequest request) {
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

            // 从token中获取deviceId
            String deviceId = authService.getDeviceIdFromToken(cleanToken);
            
            log.info("GPS数据上传: deviceId={}, Token来源: {}", deviceId, TokenUtil.getTokenSource());
            return dataService.processGpsRealtimeData(deviceId, request);
        } catch (Exception e) {
            log.error("GPS数据上传失败: {}", e.getMessage(), e);
            return ApiResponse.error("GPS数据上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/event")
    public ApiResponse<DataAck> uploadEventData(
            @Valid @RequestBody EventRequest request) {
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
            
            log.info("事件数据上传: deviceId={}, eventId={}, Token来源: {}", deviceId, request.getEventId(), TokenUtil.getTokenSource());
            return dataService.processEventData(request);
        } catch (Exception e) {
            log.error("事件数据上传失败: {}", e.getMessage(), e);
            return ApiResponse.error("事件数据上传失败: " + e.getMessage());
        }
    }

    private boolean validateToken(String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            return authService.validateToken(token);
        } catch (Exception e) {
            return false;
        }
    }
}