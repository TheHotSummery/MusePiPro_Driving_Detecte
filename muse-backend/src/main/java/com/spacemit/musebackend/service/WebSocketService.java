package com.spacemit.musebackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 发送实时设备数据
     */
    public void sendRealtimeData(String deviceId, Map<String, Object> data) {
        try {
            Map<String, Object> message = Map.of(
                "type", "realtime_data",
                "payload", Map.of(
                    "deviceId", deviceId,
                    "data", data,
                    "timestamp", System.currentTimeMillis()
                )
            );
            
            messagingTemplate.convertAndSend("/topic/realtime_data", message);
            log.debug("实时数据已发送: deviceId={}", deviceId);
        } catch (Exception e) {
            log.error("发送实时数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送设备状态更新
     */
    public void sendDeviceStatusUpdate(String deviceId, String status) {
        try {
            Map<String, Object> message = Map.of(
                "type", "device_status",
                "payload", Map.of(
                    "deviceId", deviceId,
                    "status", status,
                    "timestamp", System.currentTimeMillis()
                )
            );
            
            messagingTemplate.convertAndSend("/topic/device_status", message);
            log.info("设备状态更新已发送: deviceId={}, status={}", deviceId, status);
        } catch (Exception e) {
            log.error("发送设备状态更新失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送告警信息
     */
    public void sendAlert(String deviceId, String alertType, String message, String severity) {
        try {
            Map<String, Object> alert = Map.of(
                "type", "alert",
                "payload", Map.of(
                    "deviceId", deviceId,
                    "alertType", alertType,
                    "message", message,
                    "severity", severity,
                    "timestamp", System.currentTimeMillis()
                )
            );
            
            messagingTemplate.convertAndSend("/topic/alert", alert);
            log.info("告警信息已发送: deviceId={}, alertType={}, severity={}", deviceId, alertType, severity);
        } catch (Exception e) {
            log.error("发送告警信息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送事件通知
     */
    public void sendEvent(String deviceId, String eventType, Map<String, Object> eventData) {
        try {
            Map<String, Object> event = Map.of(
                "type", "event",
                "payload", Map.of(
                    "deviceId", deviceId,
                    "eventType", eventType,
                    "data", eventData,
                    "timestamp", System.currentTimeMillis()
                )
            );
            
            messagingTemplate.convertAndSend("/topic/event", event);
            log.info("事件通知已发送: deviceId={}, eventType={}", deviceId, eventType);
        } catch (Exception e) {
            log.error("发送事件通知失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 广播系统消息
     */
    public void broadcastSystemMessage(String message) {
        try {
            Map<String, Object> systemMessage = Map.of(
                "type", "system_message",
                "payload", Map.of(
                    "message", message,
                    "timestamp", System.currentTimeMillis()
                )
            );
            
            messagingTemplate.convertAndSend("/topic/system", systemMessage);
            log.info("系统消息已广播: {}", message);
        } catch (Exception e) {
            log.error("广播系统消息失败: {}", e.getMessage(), e);
        }
    }
}
