package com.spacemit.musebackendv2.controller.dashboard;

import com.spacemit.musebackendv2.dto.ApiResponse;
import com.spacemit.musebackendv2.dto.dashboard.TrackResponse;
import com.spacemit.musebackendv2.service.dashboard.DashboardMapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * 地图数据控制器
 * 接口路径: /api/v2/dashboard/map
 */
@RestController
@RequestMapping("/api/v2/dashboard/map")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DashboardMapController {

    private final DashboardMapService mapService;

    @PostConstruct
    public void init() {
        log.info("✅✅✅ DashboardMapController Bean已创建，路径: /api/v2/dashboard/map");
    }

    /**
     * 获取车辆实时位置
     * GET /api/v2/dashboard/map/vehicles?bounds=north,south,east,west
     */
    @GetMapping("/vehicles")
    public ApiResponse<Map<String, Object>> getMapVehicles(
            @RequestParam(required = false) String bounds) {
        try {
            Map<String, Object> response = mapService.getMapVehicles(bounds);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取车辆实时位置失败", e);
            return ApiResponse.error(500, "获取车辆实时位置失败: " + e.getMessage());
        }
    }

    /**
     * 获取车辆轨迹回放
     * GET /api/v2/dashboard/map/track?deviceId=xxx&startTime=xxx&endTime=xxx
     */
    @GetMapping("/track")
    public ApiResponse<TrackResponse> getTrack(
            @RequestParam String deviceId,
            @RequestParam Long startTime,
            @RequestParam Long endTime) {
        try {
            if (deviceId == null || deviceId.isEmpty()) {
                return ApiResponse.error(400, "deviceId参数不能为空");
            }
            if (startTime == null || endTime == null) {
                return ApiResponse.error(400, "startTime和endTime参数不能为空");
            }
            if (startTime >= endTime) {
                return ApiResponse.error(400, "startTime必须小于endTime");
            }
            
            TrackResponse response = mapService.getTrack(deviceId, startTime, endTime);
            return ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            log.warn("获取车辆轨迹失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("获取车辆轨迹失败", e);
            return ApiResponse.error(500, "获取车辆轨迹失败: " + e.getMessage());
        }
    }

    /**
     * 获取疲劳事件热力图
     * GET /api/v2/dashboard/map/heatmap?startTime=xxx&endTime=xxx&level=Level 2
     */
    @GetMapping("/heatmap")
    public ApiResponse<com.spacemit.musebackendv2.dto.dashboard.HeatmapResponse> getHeatmap(
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false) String level) {
        try {
            // 默认时间范围：最近7天
            long now = System.currentTimeMillis();
            if (startTime == null) {
                startTime = now - 7L * 24 * 60 * 60 * 1000;
            }
            if (endTime == null) {
                endTime = now;
            }
            
            com.spacemit.musebackendv2.dto.dashboard.HeatmapResponse response = mapService.getHeatmap(startTime, endTime, level);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取疲劳事件热力图失败", e);
            return ApiResponse.error(500, "获取疲劳事件热力图失败: " + e.getMessage());
        }
    }
}

