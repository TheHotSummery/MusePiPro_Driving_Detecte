package com.spacemit.musebackend.controller;

import com.spacemit.musebackend.dto.ApiResponse;
import com.spacemit.musebackend.service.PlatformService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/platform")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PlatformController {

    private final PlatformService platformService;

    /**
     * 获取系统概览
     */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> getOverview() {
        try {
            log.info("获取系统概览");
            Map<String, Object> overview = platformService.getSystemOverview();
            return ApiResponse.success("系统概览获取成功", overview);
        } catch (Exception e) {
            log.error("获取系统概览失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取系统概览失败: " + e.getMessage());
        }
    }

    /**
     * 获取设备列表
     */
    @GetMapping("/devices")
    public ApiResponse<Map<String, Object>> getDevices(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String deviceType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            log.info("获取设备列表: status={}, deviceType={}, page={}, size={}", status, deviceType, page, size);
            Map<String, Object> devices = platformService.getDevices(status, deviceType, page, size);
            return ApiResponse.success("设备列表获取成功", devices);
        } catch (Exception e) {
            log.error("获取设备列表失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取设备列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取设备详情
     */
    @GetMapping("/devices/{deviceId}")
    public ApiResponse<Map<String, Object>> getDeviceDetail(@PathVariable String deviceId) {
        try {
            log.info("获取设备详情: deviceId={}", deviceId);
            Map<String, Object> deviceDetail = platformService.getDeviceDetail(deviceId);
            return ApiResponse.success("设备详情获取成功", deviceDetail);
        } catch (Exception e) {
            log.error("获取设备详情失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取设备详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取设备历史轨迹
     */
    @GetMapping("/devices/{deviceId}/track")
    public ApiResponse<Map<String, Object>> getDeviceTrack(
            @PathVariable String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            log.info("获取设备轨迹: deviceId={}, startTime={}, endTime={}", deviceId, startTime, endTime);
            Map<String, Object> track = platformService.getDeviceTrack(deviceId, startTime, endTime);
            return ApiResponse.success("设备轨迹获取成功", track);
        } catch (Exception e) {
            log.error("获取设备轨迹失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取设备轨迹失败: " + e.getMessage());
        }
    }

    /**
     * 获取实时数据流
     */
    @GetMapping("/realtime/stream")
    public ApiResponse<Map<String, Object>> getRealtimeStream() {
        try {
            log.info("获取实时数据流");
            Map<String, Object> stream = platformService.getRealtimeStream();
            return ApiResponse.success("实时数据流获取成功", stream);
        } catch (Exception e) {
            log.error("获取实时数据流失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取实时数据流失败: " + e.getMessage());
        }
    }

    /**
     * 获取事件列表
     */
    @GetMapping("/events")
    public ApiResponse<Map<String, Object>> getEvents(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            log.info("获取事件列表: deviceId={}, eventType={}, severity={}, startTime={}, endTime={}, page={}, size={}", 
                    deviceId, eventType, severity, startTime, endTime, page, size);
            Map<String, Object> events = platformService.getEvents(deviceId, eventType, severity, startTime, endTime, page, size);
            return ApiResponse.success("事件列表获取成功", events);
        } catch (Exception e) {
            log.error("获取事件列表失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取事件列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取事件统计
     */
    @GetMapping("/events/statistics")
    public ApiResponse<Map<String, Object>> getEventStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            log.info("获取事件统计: startTime={}, endTime={}", startTime, endTime);
            Map<String, Object> statistics = platformService.getEventStatistics(startTime, endTime);
            return ApiResponse.success("事件统计获取成功", statistics);
        } catch (Exception e) {
            log.error("获取事件统计失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取事件统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取实时告警
     */
    @GetMapping("/alerts/realtime")
    public ApiResponse<Map<String, Object>> getRealtimeAlerts() {
        try {
            log.info("获取实时告警");
            Map<String, Object> alerts = platformService.getRealtimeAlerts();
            return ApiResponse.success("实时告警获取成功", alerts);
        } catch (Exception e) {
            log.error("获取实时告警失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取实时告警失败: " + e.getMessage());
        }
    }

    /**
     * 获取告警历史
     */
    @GetMapping("/alerts/history")
    public ApiResponse<Map<String, Object>> getAlertHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            log.info("获取告警历史: startTime={}, endTime={}, page={}, size={}", startTime, endTime, page, size);
            Map<String, Object> history = platformService.getAlertHistory(startTime, endTime, page, size);
            return ApiResponse.success("告警历史获取成功", history);
        } catch (Exception e) {
            log.error("获取告警历史失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取告警历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取驾驶行为分析
     */
    @GetMapping("/analysis/driving-behavior")
    public ApiResponse<Map<String, Object>> getDrivingBehaviorAnalysis(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            log.info("获取驾驶行为分析: deviceId={}, startTime={}, endTime={}", deviceId, startTime, endTime);
            Map<String, Object> analysis = platformService.getDrivingBehaviorAnalysis(deviceId, startTime, endTime);
            return ApiResponse.success("驾驶行为分析获取成功", analysis);
        } catch (Exception e) {
            log.error("获取驾驶行为分析失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取驾驶行为分析失败: " + e.getMessage());
        }
    }

    /**
     * WebSocket实时数据流订阅
     * 前端可以通过WebSocket订阅实时数据流，无需轮询
     */
    @MessageMapping("/realtime/stream")
    @SendTo("/topic/realtime_stream")
    public Map<String, Object> getRealtimeStreamWebSocket() {
        try {
            log.debug("WebSocket实时数据流请求");
            return platformService.getRealtimeStream();
        } catch (Exception e) {
            log.error("WebSocket实时数据流获取失败: {}", e.getMessage(), e);
            return Map.of("error", "实时数据流获取失败: " + e.getMessage());
        }
    }
}




