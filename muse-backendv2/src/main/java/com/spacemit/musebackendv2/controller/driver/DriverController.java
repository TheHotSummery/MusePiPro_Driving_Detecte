package com.spacemit.musebackendv2.controller.driver;

import com.spacemit.musebackendv2.dto.ApiResponse;
import com.spacemit.musebackendv2.dto.driver.DriverInfoResponse;
import com.spacemit.musebackendv2.dto.driver.DriverSafetyResponse;
import com.spacemit.musebackendv2.dto.driver.DriverTripsResponse;
import com.spacemit.musebackendv2.service.driver.DriverInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 驾驶员数据控制器
 * 接口路径: /api/v2/dashboard/driver
 */
@RestController
@RequestMapping("/api/v2/dashboard/driver")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DriverController {

    private final DriverInfoService driverInfoService;

    @PostConstruct
    public void init() {
        log.info("✅✅✅ DriverController Bean已创建，路径: /api/v2/dashboard/driver");
    }

    /**
     * 获取驾驶员基本信息
     * GET /api/v2/dashboard/driver/info?driverId=DRIVER_001
     */
    @GetMapping("/info")
    public ApiResponse<DriverInfoResponse> getDriverInfo(
            @RequestParam @NotBlank String driverId) {
        try {
            DriverInfoResponse response = driverInfoService.getDriverInfo(driverId);
            return ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            log.warn("获取驾驶员基本信息失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("获取驾驶员基本信息失败", e);
            return ApiResponse.error(500, "获取驾驶员基本信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取驾驶员行程列表
     * GET /api/v2/dashboard/driver/trips?driverId=DRIVER_001&startTime=xxx&endTime=xxx&page=1&pageSize=20
     */
    @GetMapping("/trips")
    public ApiResponse<DriverTripsResponse> getDriverTrips(
            @RequestParam @NotBlank String driverId,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            // 默认时间范围：最近30天
            long now = System.currentTimeMillis();
            if (startTime == null) {
                startTime = now - 30L * 24 * 60 * 60 * 1000;
            }
            if (endTime == null) {
                endTime = now;
            }

            // 参数验证
            if (page < 1) {
                return ApiResponse.error(400, "page参数必须大于0");
            }
            if (pageSize < 1 || pageSize > 100) {
                return ApiResponse.error(400, "pageSize参数必须在1-100之间");
            }

            DriverTripsResponse response = driverInfoService.getDriverTrips(
                driverId, startTime, endTime, page, pageSize);
            return ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            log.warn("获取驾驶员行程列表失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("获取驾驶员行程列表失败", e);
            return ApiResponse.error(500, "获取驾驶员行程列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取驾驶员安全评分
     * GET /api/v2/dashboard/driver/safety?driverId=DRIVER_001&startTime=xxx&endTime=xxx
     */
    @GetMapping("/safety")
    public ApiResponse<DriverSafetyResponse> getDriverSafety(
            @RequestParam @NotBlank String driverId,
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

            DriverSafetyResponse response = driverInfoService.getDriverSafety(
                driverId, startTime, endTime);
            return ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            log.warn("获取驾驶员安全评分失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("获取驾驶员安全评分失败", e);
            return ApiResponse.error(500, "获取驾驶员安全评分失败: " + e.getMessage());
        }
    }
}
















