package com.spacemit.musebackendv2.controller.dashboard;

import com.spacemit.musebackendv2.dto.ApiResponse;
import com.spacemit.musebackendv2.dto.dashboard.*;
import com.spacemit.musebackendv2.service.dashboard.DashboardChartsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.Arrays;

/**
 * 图表数据控制器
 * 接口路径: /api/v2/dashboard/charts
 */
@RestController
@RequestMapping("/api/v2/dashboard/charts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DashboardChartsController {

    private final DashboardChartsService chartsService;

    @PostConstruct
    public void init() {
        log.info("✅✅✅ DashboardChartsController Bean已创建，路径: /api/v2/dashboard/charts");
    }

    /**
     * 获取疲劳趋势曲线
     * GET /api/v2/dashboard/charts/trend?deviceId=xxx&startTime=xxx&endTime=xxx&interval=minute
     */
    @GetMapping("/trend")
    public ApiResponse<TrendChartResponse> getTrendChart(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "minute") String interval) {
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
            if (!Arrays.asList("minute", "hour", "day").contains(interval)) {
                return ApiResponse.error(400, "interval参数必须是: minute, hour, day");
            }
            
            TrendChartResponse response = chartsService.getTrendChart(deviceId, startTime, endTime, interval);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取疲劳趋势曲线失败", e);
            return ApiResponse.error(500, "获取疲劳趋势曲线失败: " + e.getMessage());
        }
    }

    /**
     * 获取时间段分布图
     * GET /api/v2/dashboard/charts/timeDistribution?startTime=xxx&endTime=xxx&groupBy=hour
     */
    @GetMapping("/timeDistribution")
    public ApiResponse<TimeDistributionResponse> getTimeDistribution(
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "hour") String groupBy) {
        try {
            // 默认时间范围：最近7天
            long now = System.currentTimeMillis();
            if (startTime == null) {
                startTime = now - 7L * 24 * 60 * 60 * 1000;
            }
            if (endTime == null) {
                endTime = now;
            }
            
            // 验证groupBy参数
            if (!Arrays.asList("hour", "day", "week", "month").contains(groupBy)) {
                return ApiResponse.error(400, "groupBy参数必须是: hour, day, week, month");
            }
            
            TimeDistributionResponse response = chartsService.getTimeDistribution(startTime, endTime, groupBy);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取时间段分布图失败", e);
            return ApiResponse.error(500, "获取时间段分布图失败: " + e.getMessage());
        }
    }

    /**
     * 获取行为类型分布
     * GET /api/v2/dashboard/charts/behaviorDistribution?startTime=xxx&endTime=xxx
     */
    @GetMapping("/behaviorDistribution")
    public ApiResponse<BehaviorDistributionResponse> getBehaviorDistribution(
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {
        try {
            // 默认时间范围：最近7天
            long now = System.currentTimeMillis();
            if (startTime == null) {
                startTime = now - 7L * 24 * 60 * 60 * 1000;
            }
            if (endTime == null) {
                endTime = now;
            }
            
            BehaviorDistributionResponse response = chartsService.getBehaviorDistribution(startTime, endTime);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取行为类型分布失败", e);
            return ApiResponse.error(500, "获取行为类型分布失败: " + e.getMessage());
        }
    }

    /**
     * 获取区域分布图
     * GET /api/v2/dashboard/charts/regionDistribution?startTime=xxx&endTime=xxx&level=city
     */
    @GetMapping("/regionDistribution")
    public ApiResponse<RegionDistributionResponse> getRegionDistribution(
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
            
            RegionDistributionResponse response = chartsService.getRegionDistribution(startTime, endTime, level);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取区域分布图失败", e);
            return ApiResponse.error(500, "获取区域分布图失败: " + e.getMessage());
        }
    }
}
















