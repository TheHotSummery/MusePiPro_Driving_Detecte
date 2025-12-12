package com.spacemit.musebackendv2.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spacemit.musebackendv2.dto.v2.*;
import com.spacemit.musebackendv2.entity.v2.*;
import com.spacemit.musebackendv2.repository.v2.*;
import com.spacemit.musebackendv2.service.amap.AmapGeocodeService;
import com.spacemit.musebackendv2.service.driver.TripDetectionService;
import com.spacemit.musebackendv2.util.CoordinateConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * V2版本统一数据上报服务
 * 处理硬件端上报的所有数据类型
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataReportServiceV2 {
    
    private final ObjectMapper objectMapper;
    private final DeviceV2Repository deviceRepository;
    private final DeviceDriverBindingV2Repository bindingRepository;
    private final EventDataV2Repository eventDataRepository;
    private final StatusDataV2Repository statusDataRepository;
    private final GpsDataV2Repository gpsDataRepository;
    private final TripV2Repository tripRepository;
    private final CoordinateConverter coordinateConverter;
    private final AmapGeocodeService amapGeocodeService;
    private final TripDetectionService tripDetectionService;
    
    /**
     * 处理统一数据上报
     * @param deviceId 设备ID（从Token中获取）
     * @param request 上报请求
     * @return 响应
     */
    @Transactional
    public DataReportResponse processDataReport(String deviceId, DataReportRequest request) {
        log.info("收到数据上报: deviceId={}, dataType={}, timestamp={}", deviceId, request.getDataType(), request.getTimestamp());
        
        // 更新设备最后上报时间
        updateDeviceLastReportTime(deviceId, request.getTimestamp());
        
        // 获取当前绑定的驾驶员ID
        String driverId = getCurrentDriverId(deviceId);
        
        try {
            switch (request.getDataType()) {
                case "event":
                    return processEventData(deviceId, driverId, request);
                case "status":
                    return processStatusData(deviceId, driverId, request);
                case "gps":
                    return processGpsData(deviceId, driverId, request);
                default:
                    log.warn("未知的数据类型: {}", request.getDataType());
                    throw new IllegalArgumentException("不支持的数据类型: " + request.getDataType());
            }
        } catch (Exception e) {
            log.error("处理数据上报失败: deviceId={}, dataType={}", deviceId, request.getDataType(), e);
            throw new RuntimeException("处理数据上报失败: " + e.getMessage(), e);
        }
    }

    /**
     * 异步处理行程识别
     * 检测是否开始新行程，或更新当前进行中的行程
     */
    @Async
    @Transactional
    public void processTripDetectionAsync(String deviceId, String driverId, GpsDataV2 gpsEntity) {
        try {
            // 获取当前设备最近的GPS数据（用于判断行程状态）
            List<GpsDataV2> recentGps = gpsDataRepository.findByDeviceIdAndTimestampBetween(
                deviceId, 
                gpsEntity.getTimestamp() - 10 * 60 * 1000, // 最近10分钟
                gpsEntity.getTimestamp() + 1000
            );
            
            if (recentGps.isEmpty()) {
                return;
            }
            
            // 检查是否有进行中的行程
            List<TripV2> ongoingTrips = tripRepository.findOngoingTripsByDeviceId(deviceId);
            TripV2 currentTrip = ongoingTrips.isEmpty() ? null : ongoingTrips.get(0);
            
            double currentSpeed = gpsEntity.getSpeed() != null ? gpsEntity.getSpeed().doubleValue() : 0.0;
            boolean isMoving = currentSpeed > 10.0; // 速度>10km/h认为在移动
            
            if (currentTrip == null) {
                // 没有进行中的行程，检查是否应该开始新行程
                if (isMoving) {
                    // 检查是否距离上次行程结束足够久（避免频繁启停）
                    TripV2 lastTrip = tripRepository.findByDeviceId(deviceId).stream()
                        .filter(t -> t.getEndTime() != null)
                        .max((a, b) -> Long.compare(a.getEndTime(), b.getEndTime()))
                        .orElse(null);
                    
                    boolean shouldStartTrip = true;
                    if (lastTrip != null) {
                        long timeSinceLastTrip = gpsEntity.getTimestamp() - lastTrip.getEndTime();
                        if (timeSinceLastTrip < 30 * 60 * 1000) { // 30分钟内不开始新行程
                            shouldStartTrip = false;
                        }
                    }
                    
                    if (shouldStartTrip) {
                        // 创建新行程
                        TripV2 newTrip = new TripV2();
                        newTrip.setTripId("TRIP_" + System.currentTimeMillis() + "_" + deviceId.substring(Math.max(0, deviceId.length() - 5)));
                        newTrip.setDeviceId(deviceId);
                        newTrip.setDriverId(driverId);
                        newTrip.setStartTime(gpsEntity.getTimestamp());
                        newTrip.setStartLat(gpsEntity.getLocationLat());
                        newTrip.setStartLng(gpsEntity.getLocationLng());
                        newTrip.setStatus(TripV2.TripStatus.ONGOING);
                        tripRepository.save(newTrip);
                        
                        // 更新GPS数据的tripId
                        gpsEntity.setTripId(newTrip.getTripId());
                        gpsDataRepository.save(gpsEntity);
                        
                        log.info("检测到新行程开始: deviceId={}, tripId={}", deviceId, newTrip.getTripId());
                    }
                }
            } else {
                // 有进行中的行程，更新GPS数据的tripId
                if (gpsEntity.getTripId() == null) {
                    gpsEntity.setTripId(currentTrip.getTripId());
                    gpsDataRepository.save(gpsEntity);
                }
                
                // 检查行程是否应该结束
                if (!isMoving) {
                    // 检查是否停止足够久
                    List<GpsDataV2> recentStoppedGps = recentGps.stream()
                        .filter(g -> g.getSpeed() == null || g.getSpeed().doubleValue() < 5.0)
                        .collect(java.util.stream.Collectors.toList());
                    
                    if (recentStoppedGps.size() >= 5) { // 连续5个点都停止
                        long stopDuration = gpsEntity.getTimestamp() - recentStoppedGps.get(0).getTimestamp();
                        if (stopDuration >= 5 * 60 * 1000) { // 停止超过5分钟
                            // 结束行程
                            currentTrip.setEndTime(gpsEntity.getTimestamp());
                            currentTrip.setEndLat(gpsEntity.getLocationLat());
                            currentTrip.setEndLng(gpsEntity.getLocationLng());
                            currentTrip.setStatus(TripV2.TripStatus.COMPLETED);
                            
                            // 计算行程统计
                            tripDetectionService.calculateTripStatistics(currentTrip);
                            
                            tripRepository.save(currentTrip);
                            log.info("检测到行程结束: deviceId={}, tripId={}, duration={}s", 
                                deviceId, currentTrip.getTripId(), 
                                (currentTrip.getEndTime() - currentTrip.getStartTime()) / 1000);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("行程识别处理失败: deviceId={}", deviceId, e);
            // 不抛出异常，避免影响GPS数据保存
        }
    }
    
    /**
     * 处理事件数据
     */
    private DataReportResponse processEventData(String deviceId, String driverId, DataReportRequest request) {
        try {
            EventDataDTO eventData = objectMapper.convertValue(request.getData(), EventDataDTO.class);
            
            // 幂等性检查：如果 eventId 已存在，直接返回成功
            Optional<EventDataV2> existingOpt = eventDataRepository.findByEventId(eventData.getEventId());
            if (existingOpt.isPresent()) {
                log.warn("事件数据已存在，跳过重复提交: eventId={}, deviceId={}", eventData.getEventId(), deviceId);
                return new DataReportResponse(true, System.currentTimeMillis());
            }
            
            EventDataV2 entity = new EventDataV2();
            entity.setEventId(eventData.getEventId());
            entity.setDeviceId(deviceId);
            entity.setDriverId(driverId);
            entity.setTimestamp(request.getTimestamp());
            entity.setLevel(eventData.getLevel());
            entity.setScore(eventData.getScore());
            entity.setBehavior(eventData.getBehavior());
            entity.setConfidence(eventData.getConfidence());
            entity.setDuration(eventData.getDuration());
            // GPS坐标转换：WGS84 -> GCJ02（高德地图坐标系）
            if (eventData.getLocationLat() != null && eventData.getLocationLng() != null) {
                BigDecimal[] gcj = coordinateConverter.wgs84ToGcj02(eventData.getLocationLat(), eventData.getLocationLng());
                entity.setLocationLat(gcj[0]);
                entity.setLocationLng(gcj[1]);
            } else {
                entity.setLocationLat(eventData.getLocationLat());
                entity.setLocationLng(eventData.getLocationLng());
            }
            entity.setDistractedCount(eventData.getDistractedCount() != null ? eventData.getDistractedCount() : 0);
            
            // 计算事件类型和严重程度
            entity.setEventType(calculateEventType(eventData.getBehavior()));
            entity.setSeverity(calculateSeverity(eventData.getScore()));
            
            // 先保存实体（获取ID）
            eventDataRepository.save(entity);
            
            // 异步获取地址信息（如果坐标存在且地址为空）
            if (entity.getLocationLat() != null && entity.getLocationLng() != null && 
                (entity.getLocationAddress() == null || entity.getLocationAddress().isEmpty())) {
                fillAddressAsync(entity);
            }
            
            log.info("事件数据保存成功: eventId={}, deviceId={}", eventData.getEventId(), deviceId);
            
            return new DataReportResponse(true, System.currentTimeMillis());
        } catch (Exception e) {
            log.error("处理事件数据失败: deviceId={}", deviceId, e);
            throw e;
        }
    }
    
    /**
     * 处理状态数据
     */
    private DataReportResponse processStatusData(String deviceId, String driverId, DataReportRequest request) {
        try {
            StatusDataDTO statusData = objectMapper.convertValue(request.getData(), StatusDataDTO.class);
            
            StatusDataV2 entity = new StatusDataV2();
            entity.setDeviceId(deviceId);
            entity.setDriverId(driverId);
            entity.setTimestamp(request.getTimestamp());
            entity.setLevel(statusData.getLevel());
            entity.setScore(statusData.getScore());
            // GPS坐标转换：WGS84 -> GCJ02（高德地图坐标系）
            if (statusData.getLocationLat() != null && statusData.getLocationLng() != null) {
                BigDecimal[] gcj = coordinateConverter.wgs84ToGcj02(statusData.getLocationLat(), statusData.getLocationLng());
                entity.setLocationLat(gcj[0]);
                entity.setLocationLng(gcj[1]);
            } else {
                entity.setLocationLat(statusData.getLocationLat());
                entity.setLocationLng(statusData.getLocationLng());
            }
            entity.setCpuUsage(statusData.getCpuUsage());
            entity.setMemoryUsage(statusData.getMemoryUsage());
            entity.setTemperature(statusData.getTemperature());
            
            statusDataRepository.save(entity);
            
            log.debug("状态数据保存成功: deviceId={}", deviceId);
            
            return new DataReportResponse(true, System.currentTimeMillis());
        } catch (Exception e) {
            log.error("处理状态数据失败: deviceId={}", deviceId, e);
            throw e;
        }
    }
    
    /**
     * 处理GPS数据
     */
    private DataReportResponse processGpsData(String deviceId, String driverId, DataReportRequest request) {
        try {
            GpsDataDTO gpsData = objectMapper.convertValue(request.getData(), GpsDataDTO.class);
            
            GpsDataV2 entity = new GpsDataV2();
            entity.setDeviceId(deviceId);
            entity.setDriverId(driverId);
            entity.setTimestamp(request.getTimestamp());
            
            // GPS坐标转换：WGS84 -> GCJ02（高德地图坐标系）
            if (gpsData.getLocationLat() != null && gpsData.getLocationLng() != null) {
                BigDecimal[] gcj = coordinateConverter.wgs84ToGcj02(gpsData.getLocationLat(), gpsData.getLocationLng());
                entity.setLocationLat(gcj[0]);
                entity.setLocationLng(gcj[1]);
                log.debug("GPS坐标转换: WGS84({}, {}) -> GCJ02({}, {})", 
                    gpsData.getLocationLat(), gpsData.getLocationLng(), gcj[0], gcj[1]);
            } else {
                entity.setLocationLat(gpsData.getLocationLat());
                entity.setLocationLng(gpsData.getLocationLng());
            }
            
            entity.setSpeed(gpsData.getSpeed());
            entity.setDirection(gpsData.getDirection());
            entity.setAltitude(gpsData.getAltitude());
            entity.setSatellites(gpsData.getSatellites());
            
            // 先保存实体
            gpsDataRepository.save(entity);
            
            // 异步处理行程识别和地址信息
            processTripDetectionAsync(deviceId, driverId, entity);
            
            log.debug("GPS数据保存成功: deviceId={}", deviceId);
            
            return new DataReportResponse(true, System.currentTimeMillis());
        } catch (Exception e) {
            log.error("处理GPS数据失败: deviceId={}", deviceId, e);
            throw e;
        }
    }
    
    /**
     * 更新设备最后上报时间
     */
    private void updateDeviceLastReportTime(String deviceId, Long timestamp) {
        Optional<DeviceV2> deviceOpt = deviceRepository.findByDeviceId(deviceId);
        if (deviceOpt.isPresent()) {
            DeviceV2 device = deviceOpt.get();
            if (device.getFirstReportTime() == null) {
                device.setFirstReportTime(timestamp);
            }
            device.setLastReportTime(timestamp);
            device.setStatus(DeviceV2.DeviceStatus.ONLINE);
            device.setUpdatedAt(System.currentTimeMillis());
            deviceRepository.save(device);
        } else {
            // 设备不存在，创建新设备
            DeviceV2 device = new DeviceV2();
            device.setDeviceId(deviceId);
            device.setFirstReportTime(timestamp);
            device.setLastReportTime(timestamp);
            device.setStatus(DeviceV2.DeviceStatus.ONLINE);
            device.setCreatedAt(System.currentTimeMillis());
            device.setUpdatedAt(System.currentTimeMillis());
            deviceRepository.save(device);
            log.info("自动创建新设备: deviceId={}", deviceId);
        }
    }
    
    /**
     * 获取当前绑定的驾驶员ID
     */
    private String getCurrentDriverId(String deviceId) {
        Optional<DeviceDriverBindingV2> bindingOpt = bindingRepository.findActiveBindingByDeviceId(deviceId);
        return bindingOpt.map(DeviceDriverBindingV2::getDriverId).orElse(null);
    }
    
    /**
     * 根据行为类型计算事件类型
     */
    private String calculateEventType(String behavior) {
        if (behavior == null) {
            return "EMERGENCY";
        }
        
        // 疲劳行为
        if (behavior.equals("eyes_closed") || behavior.equals("yarning") ||
            behavior.equals("eyes_closed_head_left") || behavior.equals("eyes_closed_head_right")) {
            return "FATIGUE";
        }
        
        // 分心行为
        if (behavior.equals("head_down") || behavior.equals("seeing_left") || behavior.equals("seeing_right")) {
            return "DISTRACTION";
        }
        
        return "EMERGENCY";
    }
    
    /**
     * 根据分数计算严重程度
     */
    private String calculateSeverity(BigDecimal score) {
        if (score == null) {
            return "LOW";
        }
        
        double scoreValue = score.doubleValue();
        if (scoreValue >= 85) {
            return "CRITICAL";
        } else if (scoreValue >= 70) {
            return "HIGH";
        } else if (scoreValue >= 60) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
    
    /**
     * 异步填充地址信息（用于事件数据）
     */
    @Async
    private void fillAddressAsync(EventDataV2 entity) {
        try {
            if (entity.getLocationLat() == null || entity.getLocationLng() == null) {
                return;
            }
            
            Map<String, String> addressInfo = amapGeocodeService.reverseGeocodeSync(
                entity.getLocationLat(), entity.getLocationLng());
            
            if (addressInfo != null) {
                entity.setLocationAddress(addressInfo.get("address"));
                entity.setLocationRegion(addressInfo.get("region"));
                
                // 更新数据库
                eventDataRepository.save(entity);
                
                log.debug("地址信息已填充: eventId={}, address={}", entity.getEventId(), addressInfo.get("address"));
            }
        } catch (Exception e) {
            log.warn("填充地址信息失败: eventId={}", entity.getEventId(), e);
            // 不抛出异常，避免影响主流程
        }
    }
}

