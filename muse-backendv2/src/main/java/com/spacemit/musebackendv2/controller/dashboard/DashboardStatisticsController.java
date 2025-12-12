package com.spacemit.musebackendv2.controller.dashboard;

import com.spacemit.musebackendv2.dto.ApiResponse;
import com.spacemit.musebackendv2.dto.dashboard.EventStatisticsResponse;
import com.spacemit.musebackendv2.dto.dashboard.TimeframeResponse;
import com.spacemit.musebackendv2.service.dashboard.DashboardStatisticsService;
import com.spacemit.musebackendv2.service.dashboard.DashboardDriverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.Arrays;

/**
 * 统计数据控制器
 * 接口路径: /api/v2/dashboard/statistics
 */
@RestController
@RequestMapping("/api/v2/dashboard/statistics")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DashboardStatisticsController {

    private final DashboardStatisticsService statisticsService;
    private final DashboardDriverService driverService;

    @PostConstruct
    public void init() {
        log.info("✅✅✅ DashboardStatisticsController Bean已创建，路径: /api/v2/dashboard/statistics");
    }

    /**
     * 获取疲劳事件统计
     * GET /api/v2/dashboard/statistics/events?startTime=xxx&endTime=xxx&driverId=xxx
     */
    @GetMapping("/events")
    public ApiResponse<EventStatisticsResponse> getEventStatistics(
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false) String driverId) {
        try {
            // 默认时间范围：最近7天
            long now = System.currentTimeMillis();
            if (startTime == null) {
                startTime = now - 7L * 24 * 60 * 60 * 1000;
            }
            if (endTime == null) {
                endTime = now;
            }
            
            EventStatisticsResponse response = statisticsService.getEventStatistics(startTime, endTime, driverId);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取疲劳事件统计失败", e);
            return ApiResponse.error(500, "获取疲劳事件统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取时间段分析
     * GET /api/v2/dashboard/statistics/timeframe?startTime=xxx&endTime=xxx&interval=hour
     */
    @GetMapping("/timeframe")
    public ApiResponse<TimeframeResponse> getTimeframeAnalysis(
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "hour") String interval) {
        try {
            // 默认时间范围：最近24小时
            long now = System.currentTimeMillis();
            if (startTime == null) {
                startTime = now - 24L * 60 * 60 * 1000;
            }
            if (endTime == null) {
                endTime = now;
            }
            
            // 验证interval参数
            if (!Arrays.asList("hour", "day", "week", "month").contains(interval)) {
                return ApiResponse.error(400, "interval参数必须是: hour, day, week, month");
            }
            
            TimeframeResponse response = statisticsService.getTimeframeAnalysis(startTime, endTime, interval);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取时间段分析失败", e);
            return ApiResponse.error(500, "获取时间段分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取区域分析
     * GET /api/v2/dashboard/statistics/region?startTime=xxx&endTime=xxx&level=city
     */
    @GetMapping("/region")
    public ApiResponse<com.spacemit.musebackendv2.dto.dashboard.RegionAnalysisResponse> getRegionAnalysis(
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "city") String level) {
        try {
            // 默认时间范围：最近30天
            long now = System.currentTimeMillis();
            if (startTime == null) {
                startTime = now - 30L * 24 * 60 * 60 * 1000;
            }
            if (endTime == null) {
                endTime = now;
            }
            
            // 验证level参数
            if (!Arrays.asList("province", "city", "district").contains(level)) {
                return ApiResponse.error(400, "level参数必须是: province, city, district");
            }
            
            com.spacemit.musebackendv2.dto.dashboard.RegionAnalysisResponse response = statisticsService.getRegionAnalysis(startTime, endTime, level);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取区域分析失败", e);
            return ApiResponse.error(500, "获取区域分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取驾驶员统计
     * GET /api/v2/dashboard/statistics/drivers?startTime=xxx&endTime=xxx
     */
    @GetMapping("/drivers")
    public ApiResponse<com.spacemit.musebackendv2.dto.dashboard.DriverStatisticsResponse> getDriverStatistics(
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {
        try {
            // 默认时间范围：最近30天
            long now = System.currentTimeMillis();
            if (startTime == null) {
                startTime = now - 30L * 24 * 60 * 60 * 1000;
            }
            if (endTime == null) {
                endTime = now;
            }
            
            com.spacemit.musebackendv2.dto.dashboard.DriverStatisticsResponse response = 
                driverService.getDriverStatistics(startTime, endTime);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取驾驶员统计失败", e);
            return ApiResponse.error(500, "获取驾驶员统计失败: " + e.getMessage());
        }
    }
}

