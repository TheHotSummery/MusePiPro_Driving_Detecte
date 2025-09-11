package com.spacemit.musebackend.controller;

import com.spacemit.musebackend.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketService webSocketService;

    /**
     * 处理客户端发送的消息
     */
    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public String greeting(String message) throws Exception {
        log.info("收到WebSocket消息: {}", message);
        return "Hello, " + message + "!";
    }

    /**
     * 发送实时设备数据
     */
    @GetMapping("/api/v1/websocket/test")
    @ResponseBody
    public String testWebSocket() {
        try {
            // 发送测试消息
            Map<String, Object> testData = Map.of(
                "type", "realtime_data",
                "payload", Map.of(
                    "deviceId", "TEST_DEVICE",
                    "timestamp", System.currentTimeMillis(),
                    "status", "ONLINE"
                )
            );
            
            messagingTemplate.convertAndSend("/topic/realtime_data", testData);
            log.info("WebSocket测试消息已发送");
            return "WebSocket测试消息已发送";
        } catch (Exception e) {
            log.error("发送WebSocket测试消息失败: {}", e.getMessage(), e);
            return "发送失败: " + e.getMessage();
        }
    }

    /**
     * 发送设备状态更新
     */
    public void sendDeviceStatusUpdate(String deviceId, String status) {
        try {
            Map<String, Object> statusData = Map.of(
                "type", "device_status",
                "payload", Map.of(
                    "deviceId", deviceId,
                    "status", status,
                    "timestamp", System.currentTimeMillis()
                )
            );
            
            messagingTemplate.convertAndSend("/topic/device_status", statusData);
            log.info("设备状态更新已发送: deviceId={}, status={}", deviceId, status);
        } catch (Exception e) {
            log.error("发送设备状态更新失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送告警信息
     */
    public void sendAlert(String deviceId, String alertType, String message) {
        try {
            Map<String, Object> alertData = Map.of(
                "type", "alert",
                "payload", Map.of(
                    "deviceId", deviceId,
                    "alertType", alertType,
                    "message", message,
                    "timestamp", System.currentTimeMillis(),
                    "severity", "HIGH"
                )
            );
            
            messagingTemplate.convertAndSend("/topic/alert", alertData);
            log.info("告警信息已发送: deviceId={}, alertType={}", deviceId, alertType);
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
}
