package com.spacemit.musebackendv2.service.trip;

import com.spacemit.musebackendv2.entity.v2.DeviceDriverBindingV2;
import com.spacemit.musebackendv2.entity.v2.GpsDataV2;
import com.spacemit.musebackendv2.entity.v2.TripV2;
import com.spacemit.musebackendv2.repository.v2.DeviceDriverBindingV2Repository;
import com.spacemit.musebackendv2.repository.v2.DeviceV2Repository;
import com.spacemit.musebackendv2.repository.v2.GpsDataV2Repository;
import com.spacemit.musebackendv2.repository.v2.TripV2Repository;
import com.spacemit.musebackendv2.service.driver.TripDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 行程更新定时任务
 * 定期更新进行中的行程，识别行程结束，并更新行程统计
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TripUpdateScheduler {

    private final TripV2Repository tripRepository;
    private final GpsDataV2Repository gpsDataRepository;
    private final DeviceV2Repository deviceRepository;
    private final DeviceDriverBindingV2Repository bindingRepository;
    private final TripDetectionService tripDetectionService;

    /**
     * 更新进行中的行程
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // 5分钟
    @Transactional
    public void updateOngoingTrips() {
        log.debug("开始更新进行中的行程...");
        
        try {
            // 获取所有进行中的行程
            List<TripV2> ongoingTrips = tripRepository.findAll().stream()
                .filter(t -> t.getStatus() == TripV2.TripStatus.ONGOING)
                .collect(java.util.stream.Collectors.toList());
            
            long now = System.currentTimeMillis();
            
            for (TripV2 trip : ongoingTrips) {
                try {
                    // 获取行程开始后的所有GPS数据
                    List<GpsDataV2> gpsList = gpsDataRepository.findByDeviceIdAndTimestampBetween(
                        trip.getDeviceId(),
                        trip.getStartTime(),
                        now
                    );
                    
                    if (gpsList.isEmpty()) {
                        continue;
                    }
                    
                    // 获取最近的GPS数据
                    GpsDataV2 latestGps = gpsList.stream()
                        .max((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()))
                        .orElse(null);
                    
                    if (latestGps == null) {
                        continue;
                    }
                    
                    // 检查GPS是否超时（15分钟无数据）
                    if (now - latestGps.getTimestamp() > 15 * 60 * 1000) {
                        // GPS超时，结束行程
                        trip.setEndTime(latestGps.getTimestamp());
                        trip.setEndLat(latestGps.getLocationLat());
                        trip.setEndLng(latestGps.getLocationLng());
                        trip.setStatus(TripV2.TripStatus.COMPLETED);
                        tripDetectionService.calculateTripStatistics(trip);
                        tripRepository.save(trip);
                        log.info("行程因GPS超时结束: tripId={}, deviceId={}", trip.getTripId(), trip.getDeviceId());
                        continue;
                    }
                    
                    // 检查是否停止（速度<5km/h持续5分钟）
                    double currentSpeed = latestGps.getSpeed() != null ? latestGps.getSpeed().doubleValue() : 0.0;
                    if (currentSpeed < 5.0) {
                        // 检查最近5分钟的GPS数据
                        List<GpsDataV2> recentGps = gpsList.stream()
                            .filter(g -> g.getTimestamp() >= now - 5 * 60 * 1000)
                            .filter(g -> g.getSpeed() == null || g.getSpeed().doubleValue() < 5.0)
                            .collect(java.util.stream.Collectors.toList());
                        
                        if (recentGps.size() >= 5) { // 连续5个点都停止
                            long stopStartTime = recentGps.get(0).getTimestamp();
                            if (now - stopStartTime >= 5 * 60 * 1000) { // 停止超过5分钟
                                // 结束行程
                                trip.setEndTime(stopStartTime);
                                trip.setEndLat(recentGps.get(0).getLocationLat());
                                trip.setEndLng(recentGps.get(0).getLocationLng());
                                trip.setStatus(TripV2.TripStatus.COMPLETED);
                                tripDetectionService.calculateTripStatistics(trip);
                                tripRepository.save(trip);
                                log.info("行程因停止结束: tripId={}, deviceId={}", trip.getTripId(), trip.getDeviceId());
                                continue;
                            }
                        }
                    }
                    
                    // 更新行程统计（即使未结束）
                    tripDetectionService.calculateTripStatistics(trip);
                    tripRepository.save(trip);
                    
                } catch (Exception e) {
                    log.error("更新行程失败: tripId={}", trip.getTripId(), e);
                }
            }
            
            log.debug("完成更新进行中的行程，共处理{}个行程", ongoingTrips.size());
        } catch (Exception e) {
            log.error("更新进行中的行程失败", e);
        }
    }

    /**
     * 批量处理历史GPS数据，识别行程
     * 每天凌晨2点执行一次
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点
    @Transactional
    public void processHistoricalTrips() {
        log.info("开始批量处理历史GPS数据，识别行程...");
        
        try {
            // 获取所有设备
            List<String> deviceIds = deviceRepository.findAll().stream()
                .map(d -> d.getDeviceId())
                .collect(java.util.stream.Collectors.toList());
            
            long now = System.currentTimeMillis();
            long yesterday = now - 24 * 60 * 60 * 1000; // 昨天
            
            for (String deviceId : deviceIds) {
                try {
                    // 获取昨天未关联行程的GPS数据
                    List<GpsDataV2> unprocessedGps = gpsDataRepository.findByDeviceIdAndTimestampBetween(
                        deviceId, yesterday, now
                    ).stream()
                    .filter(g -> g.getTripId() == null)
                    .collect(java.util.stream.Collectors.toList());
                    
                    if (unprocessedGps.isEmpty()) {
                        continue;
                    }
                    
                    // 识别行程
                    // 获取设备绑定的驾驶员
                    String driverId = null;
                    java.util.Optional<DeviceDriverBindingV2> bindingOpt = bindingRepository.findActiveBindingByDeviceId(deviceId);
                    if (bindingOpt.isPresent()) {
                        driverId = bindingOpt.get().getDriverId();
                    }
                    
                    List<TripV2> trips = tripDetectionService.detectTripsFromGps(
                        deviceId, driverId, yesterday, now
                    );
                    
                    // 保存行程
                    for (TripV2 trip : trips) {
                        tripRepository.save(trip);
                        
                        // 更新GPS数据的tripId
                        List<GpsDataV2> tripGps = gpsDataRepository.findByDeviceIdAndTimestampBetween(
                            deviceId, trip.getStartTime(), 
                            trip.getEndTime() != null ? trip.getEndTime() : now
                        );
                        for (GpsDataV2 gps : tripGps) {
                            if (gps.getTripId() == null) {
                                gps.setTripId(trip.getTripId());
                                gpsDataRepository.save(gps);
                            }
                        }
                    }
                    
                    log.info("处理设备历史数据完成: deviceId={}, 识别行程数={}", deviceId, trips.size());
                } catch (Exception e) {
                    log.error("处理设备历史数据失败: deviceId={}", deviceId, e);
                }
            }
            
            log.info("批量处理历史GPS数据完成");
        } catch (Exception e) {
            log.error("批量处理历史GPS数据失败", e);
        }
    }
}

