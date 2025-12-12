package com.spacemit.musebackendv2.controller.dashboard;

import com.spacemit.musebackendv2.dto.ApiResponse;
import com.spacemit.musebackendv2.dto.dashboard.RealtimeAlertsResponse;
import com.spacemit.musebackendv2.dto.dashboard.RealtimeVehiclesResponse;
import com.spacemit.musebackendv2.service.dashboard.DashboardRealtimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;

/**
 * 实时监控数据控制器
 * 接口路径: /api/v2/dashboard/realtime
 */
@RestController
@RequestMapping("/api/v2/dashboard/realtime")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DashboardRealtimeController {

    private final DashboardRealtimeService realtimeService;

    @PostConstruct
    public void init() {
        log.info("✅✅✅ DashboardRealtimeController Bean已创建，路径: /api/v2/dashboard/realtime");
    }

    /**
     * 获取实时车辆状态
     * GET /api/v2/dashboard/realtime/vehicles
     */
    @GetMapping("/vehicles")
    public ApiResponse<RealtimeVehiclesResponse> getRealtimeVehicles() {
        try {
            RealtimeVehiclesResponse response = realtimeService.getRealtimeVehicles();
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取实时车辆状态失败", e);
            return ApiResponse.error(500, "获取实时车辆状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取实时告警事件
     * GET /api/v2/dashboard/realtime/alerts
     */
    @GetMapping("/alerts")
    public ApiResponse<RealtimeAlertsResponse> getRealtimeAlerts() {
        try {
            RealtimeAlertsResponse response = realtimeService.getRealtimeAlerts();
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取实时告警事件失败", e);
            return ApiResponse.error(500, "获取实时告警事件失败: " + e.getMessage());
        }
    }

    /**
     * 获取系统运行状态
     * GET /api/v2/dashboard/realtime/system
     */
    @GetMapping("/system")
    public ApiResponse<com.spacemit.musebackendv2.dto.dashboard.SystemStatusResponse> getSystemStatus() {
        try {
            com.spacemit.musebackendv2.dto.dashboard.SystemStatusResponse response = realtimeService.getSystemStatus();
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取系统运行状态失败", e);
            return ApiResponse.error(500, "获取系统运行状态失败: " + e.getMessage());
        }
    }
}

