package com.spacemit.musebackendv2.controller.v2;

import com.spacemit.musebackendv2.dto.ApiResponse;
import com.spacemit.musebackendv2.dto.v2.DataReportRequest;
import com.spacemit.musebackendv2.dto.v2.DataReportResponse;
import com.spacemit.musebackendv2.service.AuthService;
import com.spacemit.musebackendv2.service.v2.DataReportServiceV2;
import com.spacemit.musebackendv2.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * V2版本统一数据上报控制器
 * 接口地址: POST /api/v2/data/report
 */
@RestController
@RequestMapping("/api/v2/data")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DataReportControllerV2 {
    
    private final DataReportServiceV2 dataReportService;
    private final AuthService authService;
    
    @PostConstruct
    public void init() {
        log.info("✅✅✅ DataReportControllerV2 Bean已创建，路径: /api/v2/data");
        System.out.println("✅✅✅ DataReportControllerV2 Bean已创建，路径: /api/v2/data");
    }
    
    /**
     * 统一数据上报接口
     * POST /api/v2/data/report
     * 
     * 认证方式（任选其一）：
     * - URL参数: ?token=YOUR_TOKEN
     * - Header: Authorization: Bearer YOUR_TOKEN
     * - Header: token: YOUR_TOKEN
     * 
     * 测试模式（可临时使用，跳过token验证）：
     * - URL参数: ?device_id=YOUR_DEVICE_ID
     */
    @PostMapping("/report")
    public ApiResponse<DataReportResponse> reportData(@Valid @RequestBody DataReportRequest request) {
        try {
            String deviceId = null;
            
            // 优先检查是否有device_id参数（测试模式）
            HttpServletRequest httpRequest = getHttpServletRequest();
            if (httpRequest != null) {
                String deviceIdParam = httpRequest.getParameter("device_id");
                if (deviceIdParam != null && !deviceIdParam.trim().isEmpty()) {
                    deviceId = deviceIdParam.trim();
                    log.info("使用测试模式，通过device_id参数识别设备: deviceId={}", deviceId);
                    // 测试模式下，自动查找或创建设备
                    authService.findOrCreateDeviceByDeviceId(deviceId);
                }
            }
            
            // 如果没有device_id参数，则使用token认证
            if (deviceId == null || deviceId.isEmpty()) {
                // 获取Token（支持多种方式：Authorization Header、token Header、URL参数，任选其一）
                String token = TokenUtil.getToken();
                if (!TokenUtil.isValidToken(token)) {
                    log.warn("Token无效或未提供，来源: {}", TokenUtil.getTokenSource());
                    return ApiResponse.error(401, "Token无效或未提供，或请提供device_id参数（测试模式）");
                }
                
                // 清理Token并验证
                String cleanToken = TokenUtil.cleanToken(token);
                if (!validateToken(cleanToken)) {
                    log.warn("Token验证失败，来源: {}", TokenUtil.getTokenSource());
                    return ApiResponse.error(401, "Token无效");
                }
                
                // 从token中获取deviceId
                deviceId = authService.getDeviceIdFromToken(cleanToken);
                if (deviceId == null || deviceId.isEmpty()) {
                    log.warn("无法从Token中获取设备ID");
                    return ApiResponse.error(401, "无法从Token中获取设备ID");
                }
            }
            
            log.info("数据上报请求: deviceId={}, dataType={}, timestamp={}", deviceId, request.getDataType(), request.getTimestamp());
            
            // 处理数据上报
            DataReportResponse response = dataReportService.processDataReport(deviceId, request);
            
            return ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            log.warn("数据上报参数错误: {}", e.getMessage());
            return ApiResponse.error(400, "请求参数错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("数据上报处理失败", e);
            return ApiResponse.error(500, "数据上报处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取HttpServletRequest
     */
    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest();
            }
        } catch (Exception e) {
            log.warn("获取HttpServletRequest失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 验证Token
     */
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










