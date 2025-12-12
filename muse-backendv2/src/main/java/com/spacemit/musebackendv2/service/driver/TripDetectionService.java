package com.spacemit.musebackendv2.service.driver;

import com.spacemit.musebackendv2.entity.v2.EventDataV2;
import com.spacemit.musebackendv2.entity.v2.GpsDataV2;
import com.spacemit.musebackendv2.entity.v2.TripV2;
import com.spacemit.musebackendv2.repository.v2.EventDataV2Repository;
import com.spacemit.musebackendv2.repository.v2.GpsDataV2Repository;
import com.spacemit.musebackendv2.repository.v2.TripV2Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 行程识别服务
 * 从GPS数据中自动识别行程的开始和结束
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TripDetectionService {

    private final GpsDataV2Repository gpsDataRepository;
    private final TripV2Repository tripRepository;
    private final EventDataV2Repository eventDataRepository;

    // 行程识别参数
    private static final double MIN_SPEED_FOR_START = 10.0; // 开始行程的最小速度（km/h）
    private static final double MAX_SPEED_FOR_STOP = 5.0;  // 停止的最大速度（km/h）
    private static final long MIN_STOP_DURATION = 5 * 60 * 1000; // 停止持续时间（5分钟）
    private static final long MIN_TRIP_INTERVAL = 30 * 60 * 1000; // 最小行程间隔（30分钟）
    private static final double MIN_MOVEMENT_DISTANCE = 0.1; // 最小移动距离（公里）
    private static final long GPS_TIMEOUT = 15 * 60 * 1000; // GPS超时（15分钟）

    /**
     * 从GPS数据识别行程（如果trips表为空或不完整，则实时计算）
     */
    public List<TripV2> detectTripsFromGps(String deviceId, String driverId, Long startTime, Long endTime) {
        // 获取GPS数据
        List<GpsDataV2> gpsList = gpsDataRepository.findTrackByDeviceIdAndTimeRange(deviceId, startTime, endTime);
        
        if (gpsList.isEmpty()) {
            return new ArrayList<>();
        }

        List<TripV2> trips = new ArrayList<>();
        TripV2 currentTrip = null;
        GpsDataV2 lastGps = null;
        long lastMovingTime = 0;
        boolean isMoving = false;

        for (GpsDataV2 gps : gpsList) {
            double speed = gps.getSpeed() != null ? gps.getSpeed().doubleValue() : 0.0;
            long timestamp = gps.getTimestamp();

            // 判断是否在移动
            boolean currentlyMoving = speed > MIN_SPEED_FOR_START;

            if (currentTrip == null) {
                // 寻找行程开始
                if (currentlyMoving) {
                    // 检查是否距离上次行程结束足够久
                    if (lastMovingTime == 0 || (timestamp - lastMovingTime) >= MIN_TRIP_INTERVAL) {
                        currentTrip = createNewTrip(deviceId, driverId, gps);
                        isMoving = true;
                        lastMovingTime = timestamp;
                        log.debug("检测到行程开始: deviceId={}, tripId={}, time={}", deviceId, currentTrip.getTripId(), timestamp);
                    }
                }
            } else {
                // 行程进行中
                if (currentlyMoving) {
                    // 更新最后移动时间
                    lastMovingTime = timestamp;
                    isMoving = true;
                } else {
                    // 速度降低，可能停止
                    if (isMoving) {
                        // 记录停止开始时间
                        lastMovingTime = timestamp;
                        isMoving = false;
                    } else {
                        // 检查是否停止足够久
                        if ((timestamp - lastMovingTime) >= MIN_STOP_DURATION) {
                            // 行程结束
                            completeTrip(currentTrip, lastGps != null ? lastGps : gps);
                            trips.add(currentTrip);
                            log.debug("检测到行程结束: deviceId={}, tripId={}, duration={}s", 
                                    deviceId, currentTrip.getTripId(), 
                                    (currentTrip.getEndTime() - currentTrip.getStartTime()) / 1000);
                            currentTrip = null;
                            isMoving = false;
                            lastMovingTime = timestamp;
                        }
                    }
                }
            }

            lastGps = gps;
        }

        // 处理未完成的行程
        if (currentTrip != null) {
            if (lastGps != null) {
                // 检查GPS是否超时
                long now = System.currentTimeMillis();
                if ((now - lastGps.getTimestamp()) > GPS_TIMEOUT) {
                    completeTrip(currentTrip, lastGps);
                    trips.add(currentTrip);
                    log.debug("行程因GPS超时结束: deviceId={}, tripId={}", deviceId, currentTrip.getTripId());
                } else {
                    // 标记为进行中
                    currentTrip.setStatus(TripV2.TripStatus.ONGOING);
                    trips.add(currentTrip);
                }
            }
        }

        // 计算每个行程的统计信息
        for (TripV2 trip : trips) {
            calculateTripStatistics(trip);
        }

        return trips;
    }

    /**
     * 创建新行程
     */
    private TripV2 createNewTrip(String deviceId, String driverId, GpsDataV2 startGps) {
        TripV2 trip = new TripV2();
        trip.setTripId("TRIP_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        trip.setDeviceId(deviceId);
        trip.setDriverId(driverId);
        trip.setStartTime(startGps.getTimestamp());
        trip.setStartLat(startGps.getLocationLat());
        trip.setStartLng(startGps.getLocationLng());
        trip.setStatus(TripV2.TripStatus.ONGOING);
        trip.setCreatedAt(System.currentTimeMillis());
        trip.setUpdatedAt(System.currentTimeMillis());
        return trip;
    }

    /**
     * 完成行程
     */
    private void completeTrip(TripV2 trip, GpsDataV2 endGps) {
        trip.setEndTime(endGps.getTimestamp());
        trip.setEndLat(endGps.getLocationLat());
        trip.setEndLng(endGps.getLocationLng());
        trip.setStatus(TripV2.TripStatus.COMPLETED);
        
        // 计算时长
        long duration = (trip.getEndTime() - trip.getStartTime()) / 1000; // 秒
        trip.setTotalDuration((int) duration);
        
        trip.setUpdatedAt(System.currentTimeMillis());
    }

    /**
     * 计算行程统计信息
     * 包括：总距离、总时长、事件统计、安全评分等
     */
    @Transactional
    public void calculateTripStatistics(TripV2 trip) {
        // 获取行程期间的GPS数据
        List<GpsDataV2> gpsList = gpsDataRepository.findTrackByDeviceIdAndTimeRange(
            trip.getDeviceId(), trip.getStartTime(), 
            trip.getEndTime() != null ? trip.getEndTime() : System.currentTimeMillis());

        if (gpsList.isEmpty()) {
            return;
        }

        // 计算总距离
        BigDecimal totalDistance = calculateTotalDistance(gpsList);
        trip.setTotalDistance(totalDistance);

        // 如果行程未完成，更新时长
        if (trip.getEndTime() == null) {
            long duration = (System.currentTimeMillis() - trip.getStartTime()) / 1000;
            trip.setTotalDuration((int) duration);
        } else {
            long duration = (trip.getEndTime() - trip.getStartTime()) / 1000;
            trip.setTotalDuration((int) duration);
        }

        // 统计行程内的事件
        List<EventDataV2> tripEvents = eventDataRepository.findByDeviceIdAndTimestampBetween(
            trip.getDeviceId(),
            trip.getStartTime(),
            trip.getEndTime() != null ? trip.getEndTime() : System.currentTimeMillis()
        );

        trip.setEventCount(tripEvents.size());
        trip.setCriticalEventCount((int) tripEvents.stream().filter(e -> "Level 3".equals(e.getLevel())).count());
        trip.setHighEventCount((int) tripEvents.stream().filter(e -> "Level 2".equals(e.getLevel())).count());
        trip.setMediumEventCount((int) tripEvents.stream().filter(e -> "Level 1".equals(e.getLevel())).count());
        trip.setLowEventCount((int) tripEvents.stream().filter(e -> "Normal".equals(e.getLevel())).count());

        // 计算最高级别
        String maxLevel = tripEvents.stream()
            .map(EventDataV2::getLevel)
            .filter(level -> level != null)
            .max((a, b) -> {
                int priorityA = getLevelPriority(a);
                int priorityB = getLevelPriority(b);
                return Integer.compare(priorityA, priorityB);
            })
            .orElse(null);
        trip.setMaxLevel(maxLevel);

        // 计算分数统计
        java.util.OptionalDouble maxScoreOpt = tripEvents.stream()
            .filter(e -> e.getScore() != null)
            .mapToDouble(e -> e.getScore().doubleValue())
            .max();
        java.util.OptionalDouble avgScoreOpt = tripEvents.stream()
            .filter(e -> e.getScore() != null)
            .mapToDouble(e -> e.getScore().doubleValue())
            .average();

        trip.setMaxScore(maxScoreOpt.isPresent() ? 
            BigDecimal.valueOf(maxScoreOpt.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : 
            null);
        trip.setAvgScore(avgScoreOpt.isPresent() ? 
            BigDecimal.valueOf(avgScoreOpt.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : 
            null);

        // 计算安全评分（简化算法）
        double safetyScore = 100.0;
        safetyScore -= trip.getCriticalEventCount() * 10.0;
        safetyScore -= trip.getHighEventCount() * 5.0;
        safetyScore -= trip.getMediumEventCount() * 2.0;
        if (avgScoreOpt.isPresent() && avgScoreOpt.getAsDouble() > 80) {
            safetyScore -= (avgScoreOpt.getAsDouble() - 80) * 0.5;
        }
        safetyScore = Math.max(0, Math.min(100, safetyScore));
        trip.setSafetyScore(BigDecimal.valueOf(safetyScore).setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * 获取级别优先级（用于排序）
     */
    private int getLevelPriority(String level) {
        if ("Level 3".equals(level)) return 3;
        if ("Level 2".equals(level)) return 2;
        if ("Level 1".equals(level)) return 1;
        return 0;
    }

    /**
     * 计算总距离（Haversine公式）
     */
    private BigDecimal calculateTotalDistance(List<GpsDataV2> gpsList) {
        if (gpsList.size() < 2) {
            return BigDecimal.ZERO;
        }

        double totalDistance = 0.0; // 米
        for (int i = 0; i < gpsList.size() - 1; i++) {
            GpsDataV2 p1 = gpsList.get(i);
            GpsDataV2 p2 = gpsList.get(i + 1);
            
            if (p1.getLocationLat() != null && p1.getLocationLng() != null &&
                p2.getLocationLat() != null && p2.getLocationLng() != null) {
                double distance = haversine(
                    p1.getLocationLat().doubleValue(), p1.getLocationLng().doubleValue(),
                    p2.getLocationLat().doubleValue(), p2.getLocationLng().doubleValue()
                );
                totalDistance += distance;
            }
        }
        
        return BigDecimal.valueOf(totalDistance / 1000.0).setScale(2, RoundingMode.HALF_UP); // 转换为公里
    }

    /**
     * Haversine公式计算两点间距离（米）
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // 地球半径（米）

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}

