package com.spacemit.musebackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 推送实时数据更新
     */
    public void pushRealtimeDataUpdate(String deviceId, Map<String, Object> data) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "realtime_data");
            message.put("deviceId", deviceId);
            message.put("data", data);
            message.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSend("/topic/realtime_data", message);
            log.debug("推送实时数据更新: deviceId={}", deviceId);
        } catch (Exception e) {
            log.error("推送实时数据更新失败: deviceId={}, 错误: {}", deviceId, e.getMessage(), e);
        }
    }

    /**
     * 推送设备状态更新
     */
    public void pushDeviceStatusUpdate(String deviceId, String status) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "device_status");
            message.put("deviceId", deviceId);
            message.put("status", status);
            message.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSend("/topic/device_status", message);
            log.debug("推送设备状态更新: deviceId={}, status={}", deviceId, status);
        } catch (Exception e) {
            log.error("推送设备状态更新失败: deviceId={}, 错误: {}", deviceId, e.getMessage(), e);
        }
    }

    /**
     * 推送告警信息
     */
    public void pushAlert(String deviceId, String alertType, String severity, Map<String, Object> alertData) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "alert");
            message.put("deviceId", deviceId);
            message.put("alertType", alertType);
            message.put("severity", severity);
            message.put("data", alertData);
            message.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSend("/topic/alert", message);
            log.debug("推送告警信息: deviceId={}, alertType={}, severity={}", deviceId, alertType, severity);
        } catch (Exception e) {
            log.error("推送告警信息失败: deviceId={}, 错误: {}", deviceId, e.getMessage(), e);
        }
    }

    /**
     * 推送事件通知
     */
    public void pushEvent(String deviceId, String eventType, Map<String, Object> eventData) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "event");
            message.put("deviceId", deviceId);
            message.put("eventType", eventType);
            message.put("data", eventData);
            message.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSend("/topic/event", message);
            log.debug("推送事件通知: deviceId={}, eventType={}", deviceId, eventType);
        } catch (Exception e) {
            log.error("推送事件通知失败: deviceId={}, 错误: {}", deviceId, e.getMessage(), e);
        }
    }

    /**
     * 推送GPS数据更新
     */
    public void pushGpsDataUpdate(String deviceId, Map<String, Object> gpsData) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "gps_data");
            message.put("deviceId", deviceId);
            message.put("data", gpsData);
            message.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSend("/topic/gps_data", message);
            log.debug("推送GPS数据更新: deviceId={}", deviceId);
        } catch (Exception e) {
            log.error("推送GPS数据更新失败: deviceId={}, 错误: {}", deviceId, e.getMessage(), e);
        }
    }

    /**
     * 推送系统概览更新
     */
    public void pushSystemOverviewUpdate(Map<String, Object> overview) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "system_overview");
            message.put("data", overview);
            message.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSend("/topic/system_overview", message);
            log.debug("推送系统概览更新");
        } catch (Exception e) {
            log.error("推送系统概览更新失败: {}", e.getMessage(), e);
        }
    }
}

