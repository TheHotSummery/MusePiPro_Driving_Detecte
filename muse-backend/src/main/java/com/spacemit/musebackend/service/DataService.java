package com.spacemit.musebackend.service;

import com.spacemit.musebackend.dto.*;
import com.spacemit.musebackend.entity.Event;
import com.spacemit.musebackend.entity.GpsData;
import com.spacemit.musebackend.entity.RealtimeData;
import com.spacemit.musebackend.repository.EventRepository;
import com.spacemit.musebackend.repository.GpsDataRepository;
import com.spacemit.musebackend.repository.RealtimeDataRepository;
import com.spacemit.musebackend.util.GpsDataParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataService {

    private final RealtimeDataRepository realtimeDataRepository;
    private final GpsDataRepository gpsDataRepository;
    private final EventRepository eventRepository;
    private final GpsDataParser gpsDataParser;
    private final FatigueAnalysisService fatigueAnalysisService;
    private final WebSocketNotificationService webSocketNotificationService;

    public ApiResponse<DataAck> processRealtimeData(RealtimeDataRequest request) {
        try {
            // 查找该设备是否已有实时数据记录
            RealtimeData existingData = realtimeDataRepository.findTopByDeviceIdOrderByTimestampDesc(request.getDeviceId()).orElse(null);
            
            RealtimeData data;
            if (existingData != null) {
                // 更新现有记录
                data = existingData;
                log.debug("更新现有实时数据记录: deviceId={}", request.getDeviceId());
            } else {
                // 创建新记录
                data = new RealtimeData();
                log.debug("创建新实时数据记录: deviceId={}", request.getDeviceId());
            }
            
            // 更新数据
            data.setDeviceId(request.getDeviceId());
            data.setTimestamp(request.getTimestamp());
            data.setLocationLat(request.getLocationLat());
            data.setLocationLng(request.getLocationLng());
            data.setSpeed(request.getSpeed());
            data.setDirection(request.getDirection());
            data.setAltitude(request.getAltitude());
            data.setHdop(request.getHdop());
            data.setSatellites(request.getSatellites());
            data.setFixMode(request.getFixMode());

            realtimeDataRepository.save(data);

            // 推送实时数据更新到WebSocket
            Map<String, Object> realtimeData = new HashMap<>();
            realtimeData.put("timestamp", data.getTimestamp());
            realtimeData.put("locationLat", data.getLocationLat());
            realtimeData.put("locationLng", data.getLocationLng());
            realtimeData.put("speed", data.getSpeed());
            realtimeData.put("direction", data.getDirection());
            realtimeData.put("altitude", data.getAltitude());
            realtimeData.put("hdop", data.getHdop());
            realtimeData.put("satellites", data.getSatellites());
            realtimeData.put("fixMode", data.getFixMode());
            
            webSocketNotificationService.pushRealtimeDataUpdate(request.getDeviceId(), realtimeData);

            DataAck ack = new DataAck();
            ack.setMessageId(UUID.randomUUID().toString());
            ack.setSuccess(true);
            ack.setMessage("实时数据接收成功");

            log.info("实时数据接收成功: deviceId={}, timestamp={}, 操作={}", 
                    request.getDeviceId(), request.getTimestamp(), 
                    existingData != null ? "更新" : "创建");

            return ApiResponse.success("实时数据接收成功", ack);
        } catch (Exception e) {
            log.error("实时数据接收失败: {}", e.getMessage(), e);
            return ApiResponse.error("实时数据接收失败: " + e.getMessage());
        }
    }

    /**
     * 处理GPS实时数据（新版本，接收原始GPS数据）
     */
    public ApiResponse<DataAck> processGpsRealtimeData(String deviceId, RealtimeGpsRequest request) {
        try {
            // 解析GPS原始数据
            GpsDataParser.ParsedGpsData parsedData = gpsDataParser.parseGpsData(request.getRawGpsData());
            
            // 验证GPS数据质量
            if (!gpsDataParser.isGpsDataValid(parsedData)) {
                log.warn("GPS数据质量不佳，设备: {}", deviceId);
                return ApiResponse.error("GPS数据质量不佳，请检查GPS信号");
            }
            
            // 分析疲劳驾驶状态
            GpsData.FatigueLevel fatigueLevel = fatigueAnalysisService.analyzeFatigueLevel(
                request.getFatigueScore(),
                request.getEyeBlinkRate(),
                request.getHeadMovementScore(),
                request.getYawnCount(),
                request.getAttentionScore()
            );
            
            // 查找该设备是否已有GPS数据记录
            GpsData existingGpsData = gpsDataRepository.findTopByDeviceIdOrderByTimestampDesc(deviceId).orElse(null);
            
            GpsData gpsData;
            if (existingGpsData != null) {
                // 更新现有记录
                gpsData = existingGpsData;
                log.debug("更新现有GPS数据记录: deviceId={}", deviceId);
            } else {
                // 创建新记录
                gpsData = new GpsData();
                log.debug("创建新GPS数据记录: deviceId={}", deviceId);
            }
            
            // 设置GPS数据
            gpsData.setDeviceId(deviceId);
            gpsData.setTimestamp(parsedData.getTimestamp());
            gpsData.setUtcTime(parsedData.getUtcTime());
            gpsData.setUtcDate(parsedData.getUtcDate());
            gpsData.setLatitude(parsedData.getLatitude());
            gpsData.setLongitude(parsedData.getLongitude());
            gpsData.setHdop(parsedData.getHdop());
            gpsData.setAltitude(parsedData.getAltitude());
            gpsData.setFixMode(parsedData.getFixMode());
            gpsData.setCourseOverGround(parsedData.getCourseOverGround());
            gpsData.setSpeedKmh(parsedData.getSpeedKmh());
            gpsData.setSpeedKnots(parsedData.getSpeedKnots());
            gpsData.setSatellites(parsedData.getSatellites());
            gpsData.setRawGpsData(request.getRawGpsData());
            
            // 设置疲劳驾驶数据
            gpsData.setFatigueScore(request.getFatigueScore());
            gpsData.setFatigueLevel(fatigueLevel);
            gpsData.setEyeBlinkRate(request.getEyeBlinkRate());
            gpsData.setHeadMovementScore(request.getHeadMovementScore());
            gpsData.setYawnCount(request.getYawnCount());
            gpsData.setAttentionScore(request.getAttentionScore());
            
            // 保存GPS数据
            gpsDataRepository.save(gpsData);
            
            // 同步GPS数据到realtime_data表
            syncGpsToRealtimeData(deviceId, parsedData);
            
            // 推送GPS数据更新到WebSocket
            Map<String, Object> gpsUpdateData = new HashMap<>();
            gpsUpdateData.put("timestamp", parsedData.getTimestamp());
            gpsUpdateData.put("latitude", parsedData.getLatitude());
            gpsUpdateData.put("longitude", parsedData.getLongitude());
            gpsUpdateData.put("speedKmh", parsedData.getSpeedKmh());
            gpsUpdateData.put("courseOverGround", parsedData.getCourseOverGround());
            gpsUpdateData.put("altitude", parsedData.getAltitude());
            gpsUpdateData.put("hdop", parsedData.getHdop());
            gpsUpdateData.put("satellites", parsedData.getSatellites());
            gpsUpdateData.put("fatigueLevel", fatigueLevel);
            gpsUpdateData.put("fatigueScore", request.getFatigueScore());
            
            webSocketNotificationService.pushGpsDataUpdate(deviceId, gpsUpdateData);
            
            // 检查是否需要触发疲劳驾驶事件
            if (fatigueAnalysisService.shouldTriggerFatigueEvent(fatigueLevel, parsedData.getSpeedKmh())) {
                createFatigueEvent(deviceId, gpsData, fatigueLevel);
            }
            
            DataAck ack = new DataAck();
            ack.setMessageId(UUID.randomUUID().toString());
            ack.setSuccess(true);
            ack.setMessage("GPS数据接收成功");
            
            String gpsQuality = gpsDataParser.getGpsQualityRating(parsedData);
            log.info("GPS数据接收成功: deviceId={}, 质量={}, 疲劳等级={}, 卫星数={}", 
                    deviceId, gpsQuality, fatigueLevel, parsedData.getSatellites());
            
            return ApiResponse.success("GPS数据接收成功", ack);
            
        } catch (Exception e) {
            log.error("GPS数据接收失败: {}", e.getMessage(), e);
            return ApiResponse.error("GPS数据接收失败: " + e.getMessage());
        }
    }

    /**
     * 创建疲劳驾驶事件
     */
    private void createFatigueEvent(String deviceId, GpsData gpsData, GpsData.FatigueLevel fatigueLevel) {
        try {
            Event event = new Event();
            event.setEventId("FATIGUE_" + deviceId + "_" + System.currentTimeMillis());
            event.setDeviceId(deviceId);
            event.setTimestamp(gpsData.getTimestamp());
            event.setEventType(Event.EventType.FATIGUE);
            
            // 根据疲劳等级设置严重程度
            switch (fatigueLevel) {
                case MODERATE:
                    event.setSeverity(Event.Severity.HIGH);
                    break;
                case SEVERE:
                    event.setSeverity(Event.Severity.CRITICAL);
                    break;
                default:
                    event.setSeverity(Event.Severity.MEDIUM);
                    break;
            }
            
            event.setLocationLat(gpsData.getLatitude());
            event.setLocationLng(gpsData.getLongitude());
            event.setBehavior("FATIGUE_DETECTED");
            event.setConfidence(gpsData.getFatigueScore());
            event.setAlertLevel(fatigueLevel.toString());
            event.setContext(fatigueAnalysisService.getFatigueLevelDescription(fatigueLevel) + 
                           " | 建议: " + fatigueAnalysisService.getFatigueAdvice(fatigueLevel));
            
            eventRepository.save(event);
            
            // 推送疲劳驾驶事件到WebSocket
            Map<String, Object> fatigueEventData = new HashMap<>();
            fatigueEventData.put("eventId", event.getEventId());
            fatigueEventData.put("timestamp", event.getTimestamp());
            fatigueEventData.put("eventType", event.getEventType());
            fatigueEventData.put("severity", event.getSeverity());
            fatigueEventData.put("locationLat", event.getLocationLat());
            fatigueEventData.put("locationLng", event.getLocationLng());
            fatigueEventData.put("behavior", event.getBehavior());
            fatigueEventData.put("confidence", event.getConfidence());
            fatigueEventData.put("alertLevel", event.getAlertLevel());
            fatigueEventData.put("context", event.getContext());
            fatigueEventData.put("fatigueLevel", fatigueLevel);
            
            webSocketNotificationService.pushEvent(deviceId, "FATIGUE", fatigueEventData);
            webSocketNotificationService.pushAlert(deviceId, "FATIGUE", event.getSeverity().toString(), fatigueEventData);
            
            log.warn("疲劳驾驶事件已创建: deviceId={}, 疲劳等级={}, 严重程度={}", 
                    deviceId, fatigueLevel, event.getSeverity());
            
        } catch (Exception e) {
            log.error("创建疲劳驾驶事件失败: {}", e.getMessage(), e);
        }
    }

    public ApiResponse<DataAck> processEventData(EventRequest request) {
        try {
            // 检查事件ID是否已存在
            Optional<Event> existingEvent = eventRepository.findByEventId(request.getEventId());
            if (existingEvent.isPresent()) {
                log.warn("事件ID已存在，跳过重复事件: eventId={}", request.getEventId());
                DataAck ack = new DataAck();
                ack.setMessageId(UUID.randomUUID().toString());
                ack.setSuccess(true);
                ack.setMessage("事件已存在，跳过重复提交");
                return ApiResponse.success("事件已存在，跳过重复提交", ack);
            }

            Event event = new Event();
            event.setEventId(request.getEventId());
            event.setDeviceId(request.getDeviceId());
            event.setTimestamp(request.getTimestamp());
            event.setEventType(Event.EventType.valueOf(request.getEventType().toUpperCase()));
            event.setSeverity(Event.Severity.valueOf(request.getSeverity().toUpperCase()));
            event.setLocationLat(request.getLocationLat());
            event.setLocationLng(request.getLocationLng());
            event.setBehavior(request.getBehavior());
            event.setConfidence(request.getConfidence());
            
            // 为缺失字段提供默认值
            event.setDuration(request.getDuration() != null ? request.getDuration() : BigDecimal.ZERO);
            event.setAlertLevel(request.getAlertLevel() != null ? request.getAlertLevel() : generateDefaultAlertLevel(request.getSeverity()));
            event.setGpioTriggered(request.getGpioTriggered() != null ? request.getGpioTriggered() : "{}");
            event.setContext(request.getContext() != null ? request.getContext() : generateDefaultContext(request.getEventType(), request.getBehavior()));

            eventRepository.save(event);

            // 推送事件通知到WebSocket
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("eventId", event.getEventId());
            eventData.put("timestamp", event.getTimestamp());
            eventData.put("eventType", event.getEventType());
            eventData.put("severity", event.getSeverity());
            eventData.put("locationLat", event.getLocationLat());
            eventData.put("locationLng", event.getLocationLng());
            eventData.put("behavior", event.getBehavior());
            eventData.put("confidence", event.getConfidence());
            eventData.put("alertLevel", event.getAlertLevel());
            eventData.put("context", event.getContext());
            
            webSocketNotificationService.pushEvent(request.getDeviceId(), request.getEventType(), eventData);
            
            // 如果是高严重程度的事件，也推送告警
            if (event.getSeverity() == Event.Severity.HIGH || event.getSeverity() == Event.Severity.CRITICAL) {
                webSocketNotificationService.pushAlert(request.getDeviceId(), request.getEventType(), 
                    event.getSeverity().toString(), eventData);
            }

            DataAck ack = new DataAck();
            ack.setMessageId(UUID.randomUUID().toString());
            ack.setSuccess(true);
            ack.setMessage("事件数据接收成功");

            log.info("事件数据接收成功: deviceId={}, eventId={}, eventType={}",
                    request.getDeviceId(), request.getEventId(), request.getEventType());

            return ApiResponse.success("事件数据接收成功", ack);
        } catch (Exception e) {
            log.error("事件数据接收失败: {}", e.getMessage(), e);
            return ApiResponse.error("事件数据接收失败: " + e.getMessage());
        }
    }

    /**
     * 生成默认告警级别
     */
    private String generateDefaultAlertLevel(String severity) {
        switch (severity.toUpperCase()) {
            case "LOW":
                return "LOW";
            case "MEDIUM":
                return "MEDIUM";
            case "HIGH":
                return "HIGH";
            case "CRITICAL":
                return "CRITICAL";
            default:
                return "MEDIUM";
        }
    }

    /**
     * 生成默认上下文描述
     */
    private String generateDefaultContext(String eventType, String behavior) {
        StringBuilder context = new StringBuilder();
        
        switch (eventType.toUpperCase()) {
            case "FATIGUE":
                context.append("疲劳驾驶检测");
                if (behavior != null) {
                    switch (behavior.toLowerCase()) {
                        case "eye_closed":
                            context.append("：检测到闭眼行为");
                            break;
                        case "yawn":
                            context.append("：检测到打哈欠行为");
                            break;
                        case "head_nod":
                            context.append("：检测到点头行为");
                            break;
                        default:
                            context.append("：检测到疲劳行为");
                    }
                }
                break;
            case "DISTRACTION":
                context.append("分心驾驶检测");
                if (behavior != null) {
                    switch (behavior.toLowerCase()) {
                        case "phone_usage":
                            context.append("：检测到手机使用行为");
                            break;
                        case "looking_away":
                            context.append("：检测到视线偏离行为");
                            break;
                        default:
                            context.append("：检测到分心行为");
                    }
                }
                break;
            case "EMERGENCY":
                context.append("紧急事件检测");
                if (behavior != null) {
                    switch (behavior.toLowerCase()) {
                        case "sudden_brake":
                            context.append("：检测到急刹车行为");
                            break;
                        case "collision":
                            context.append("：检测到碰撞事件");
                            break;
                        default:
                            context.append("：检测到紧急事件");
                    }
                }
                break;
            default:
                context.append("系统事件：").append(eventType);
        }
        
        return context.toString();
    }
    
    /**
     * 同步GPS数据到realtime_data表
     */
    private void syncGpsToRealtimeData(String deviceId, GpsDataParser.ParsedGpsData parsedData) {
        try {
            // 查找该设备是否已有实时数据记录
            RealtimeData existingData = realtimeDataRepository.findTopByDeviceIdOrderByTimestampDesc(deviceId).orElse(null);
            
            RealtimeData realtimeData;
            if (existingData != null) {
                // 更新现有记录
                realtimeData = existingData;
                log.debug("同步GPS数据到现有实时数据记录: deviceId={}", deviceId);
            } else {
                // 创建新记录
                realtimeData = new RealtimeData();
                log.debug("同步GPS数据到新实时数据记录: deviceId={}", deviceId);
            }
            
            // 更新实时数据
            realtimeData.setDeviceId(deviceId);
            realtimeData.setTimestamp(parsedData.getTimestamp());
            realtimeData.setLocationLat(parsedData.getLatitude());
            realtimeData.setLocationLng(parsedData.getLongitude());
            realtimeData.setSpeed(parsedData.getSpeedKmh());
            realtimeData.setDirection(parsedData.getCourseOverGround());
            realtimeData.setAltitude(parsedData.getAltitude());
            realtimeData.setHdop(parsedData.getHdop());
            realtimeData.setSatellites(parsedData.getSatellites());
            realtimeData.setFixMode(parsedData.getFixMode());
            
            realtimeDataRepository.save(realtimeData);
            
            // 推送实时数据更新到WebSocket（GPS同步后）
            Map<String, Object> realtimeDataMap = new HashMap<>();
            realtimeDataMap.put("timestamp", realtimeData.getTimestamp());
            realtimeDataMap.put("locationLat", realtimeData.getLocationLat());
            realtimeDataMap.put("locationLng", realtimeData.getLocationLng());
            realtimeDataMap.put("speed", realtimeData.getSpeed());
            realtimeDataMap.put("direction", realtimeData.getDirection());
            realtimeDataMap.put("altitude", realtimeData.getAltitude());
            realtimeDataMap.put("hdop", realtimeData.getHdop());
            realtimeDataMap.put("satellites", realtimeData.getSatellites());
            realtimeDataMap.put("fixMode", realtimeData.getFixMode());
            realtimeDataMap.put("source", "gps_sync");
            
            webSocketNotificationService.pushRealtimeDataUpdate(deviceId, realtimeDataMap);
            
            log.info("GPS数据同步到实时数据表成功: deviceId={}, 坐标=({}, {}), 操作={}", 
                    deviceId, parsedData.getLatitude(), parsedData.getLongitude(),
                    existingData != null ? "更新" : "创建");
                    
        } catch (Exception e) {
            log.error("同步GPS数据到实时数据表失败: deviceId={}, 错误: {}", deviceId, e.getMessage(), e);
        }
    }
}