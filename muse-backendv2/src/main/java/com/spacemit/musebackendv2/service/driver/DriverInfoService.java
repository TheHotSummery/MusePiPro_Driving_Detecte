package com.spacemit.musebackendv2.service.driver;

import com.spacemit.musebackendv2.dto.driver.DriverInfoResponse;
import com.spacemit.musebackendv2.dto.driver.DriverSafetyResponse;
import com.spacemit.musebackendv2.dto.driver.DriverTripsResponse;
import com.spacemit.musebackendv2.entity.v2.*;
import com.spacemit.musebackendv2.repository.v2.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 驾驶员信息服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DriverInfoService {

    private final DriverV2Repository driverRepository;
    private final TeamV2Repository teamRepository;
    private final DeviceDriverBindingV2Repository bindingRepository;
    private final TripV2Repository tripRepository;
    private final EventDataV2Repository eventDataRepository;
    private final SafetyScoreService safetyScoreService;
    private final TripDetectionService tripDetectionService;

    /**
     * 获取驾驶员基本信息
     */
    public DriverInfoResponse getDriverInfo(String driverId) {
        Optional<DriverV2> driverOpt = driverRepository.findByDriverId(driverId);
        if (!driverOpt.isPresent()) {
            throw new IllegalArgumentException("驾驶员不存在: " + driverId);
        }

        DriverV2 driver = driverOpt.get();

        // 获取车队信息
        String teamName = null;
        if (driver.getTeamId() != null) {
            Optional<TeamV2> teamOpt = teamRepository.findByTeamId(driver.getTeamId());
            if (teamOpt.isPresent()) {
                teamName = teamOpt.get().getTeamName();
            }
        }

        // 获取绑定的设备
        List<DeviceDriverBindingV2> bindings = bindingRepository.findByDriverId(driverId);
        List<DriverInfoResponse.BindDeviceDTO> bindDevices = bindings.stream()
            .map(binding -> {
                DriverInfoResponse.BindDeviceDTO device = new DriverInfoResponse.BindDeviceDTO();
                device.setDeviceId(binding.getDeviceId());
                device.setDeviceName("设备" + binding.getDeviceId()); // 简化，实际应从Device表获取
                device.setBindTime(binding.getBindTime());
                device.setStatus(binding.getStatus() == DeviceDriverBindingV2.BindingStatus.ACTIVE ? "active" : "inactive");
                return device;
            })
            .collect(Collectors.toList());

        // 计算统计数据（全部历史数据）
        List<TripV2> allTrips = tripRepository.findByDriverId(driverId);
        BigDecimal totalDistance = allTrips.stream()
            .filter(t -> t.getTotalDistance() != null)
            .map(TripV2::getTotalDistance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalDuration = allTrips.stream()
            .filter(t -> t.getTotalDuration() != null)
            .mapToInt(TripV2::getTotalDuration)
            .sum();

        List<EventDataV2> allEvents = eventDataRepository.findByDriverId(driverId);
        
        // 计算安全评分（最近30天）
        long endTime = System.currentTimeMillis();
        long startTime = endTime - 30L * 24 * 60 * 60 * 1000;
        BigDecimal safetyScore = safetyScoreService.calculateSafetyScore(driverId, startTime, endTime)
            .getOverallScore();

        // 加入日期（简化：使用创建时间）
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String joinDate = driver.getCreatedAt() != null ? 
            sdf.format(new Date(driver.getCreatedAt())) : null;

        DriverInfoResponse.DriverStatisticsDTO statistics = new DriverInfoResponse.DriverStatisticsDTO();
        statistics.setTotalTrips(allTrips.size());
        statistics.setTotalDistance(totalDistance);
        statistics.setTotalDuration(totalDuration);
        statistics.setTotalEvents(allEvents.size());
        statistics.setSafetyScore(safetyScore);
        statistics.setJoinDate(joinDate);

        DriverInfoResponse response = new DriverInfoResponse();
        response.setDriverId(driver.getDriverId());
        response.setDriverName(driver.getDriverName());
        response.setPhone(driver.getPhone());
        response.setEmail(driver.getEmail());
        response.setLicenseNumber(driver.getLicenseNumber());
        response.setLicenseType(driver.getLicenseType());
        response.setLicenseExpire(driver.getLicenseExpire() != null ? 
            driver.getLicenseExpire().toString() : null);
        response.setAvatar(driver.getAvatarUrl());
        response.setTeamName(teamName);
        response.setBindDevices(bindDevices);
        response.setStatistics(statistics);

        return response;
    }

    /**
     * 获取驾驶员行程列表
     */
    public DriverTripsResponse getDriverTrips(String driverId, Long startTime, Long endTime, 
                                              int page, int pageSize) {
        Optional<DriverV2> driverOpt = driverRepository.findByDriverId(driverId);
        if (!driverOpt.isPresent()) {
            throw new IllegalArgumentException("驾驶员不存在: " + driverId);
        }

        DriverV2 driver = driverOpt.get();

        // 获取行程（如果trips表为空，则从GPS数据实时计算）
        List<TripV2> trips;
        List<TripV2> dbTrips = tripRepository.findByDriverIdAndStartTimeBetween(driverId, startTime, endTime);
        
        if (dbTrips.isEmpty()) {
            // 从GPS数据识别行程
            String deviceId = getDeviceIdByDriverId(driverId);
            if (deviceId != null) {
                trips = tripDetectionService.detectTripsFromGps(deviceId, driverId, startTime, endTime);
            } else {
                trips = Collections.emptyList();
            }
        } else {
            trips = dbTrips;
        }

        // 分页
        int total = trips.size();
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        List<TripV2> pagedTrips = start < total ? trips.subList(start, end) : Collections.emptyList();

        // 转换为DTO
        List<DriverTripsResponse.TripDTO> tripDTOs = pagedTrips.stream()
            .map(trip -> {
                DriverTripsResponse.TripDTO dto = new DriverTripsResponse.TripDTO();
                dto.setTripId(trip.getTripId());
                dto.setDeviceId(trip.getDeviceId());
                dto.setStartTime(trip.getStartTime());
                dto.setEndTime(trip.getEndTime());
                dto.setDuration(trip.getTotalDuration());
                dto.setDistance(trip.getTotalDistance());

                // 起点位置
                DriverTripsResponse.LocationDTO startLoc = new DriverTripsResponse.LocationDTO();
                startLoc.setLat(trip.getStartLat());
                startLoc.setLng(trip.getStartLng());
                startLoc.setAddress(trip.getStartAddress());
                dto.setStartLocation(startLoc);

                // 终点位置
                DriverTripsResponse.LocationDTO endLoc = new DriverTripsResponse.LocationDTO();
                endLoc.setLat(trip.getEndLat());
                endLoc.setLng(trip.getEndLng());
                endLoc.setAddress(trip.getEndAddress());
                dto.setEndLocation(endLoc);

                // 行程统计
                List<EventDataV2> tripEvents = eventDataRepository.findByDriverIdAndTimestampBetween(
                    driverId, trip.getStartTime(), 
                    trip.getEndTime() != null ? trip.getEndTime() : System.currentTimeMillis());

                DriverTripsResponse.TripStatisticsDTO stats = new DriverTripsResponse.TripStatisticsDTO();
                stats.setEventCount(tripEvents.size());
                stats.setCriticalEvents((int) tripEvents.stream().filter(e -> "Level 3".equals(e.getLevel())).count());
                stats.setHighEvents((int) tripEvents.stream().filter(e -> "Level 2".equals(e.getLevel())).count());
                stats.setMediumEvents((int) tripEvents.stream().filter(e -> "Level 1".equals(e.getLevel())).count());
                stats.setLowEvents((int) tripEvents.stream().filter(e -> "Normal".equals(e.getLevel())).count());

                OptionalDouble maxScoreOpt = tripEvents.stream()
                    .filter(e -> e.getScore() != null)
                    .mapToDouble(e -> e.getScore().doubleValue())
                    .max();
                OptionalDouble avgScoreOpt = tripEvents.stream()
                    .filter(e -> e.getScore() != null)
                    .mapToDouble(e -> e.getScore().doubleValue())
                    .average();

                stats.setMaxScore(maxScoreOpt.isPresent() ? 
                    BigDecimal.valueOf(maxScoreOpt.getAsDouble()).setScale(2, java.math.RoundingMode.HALF_UP) : 
                    BigDecimal.ZERO);
                stats.setAvgScore(avgScoreOpt.isPresent() ? 
                    BigDecimal.valueOf(avgScoreOpt.getAsDouble()).setScale(2, java.math.RoundingMode.HALF_UP) : 
                    BigDecimal.ZERO);

                // 计算行程安全评分
                SafetyScoreService.SafetyScoreResult tripScore = safetyScoreService.calculateSafetyScore(
                    driverId, trip.getStartTime(), 
                    trip.getEndTime() != null ? trip.getEndTime() : System.currentTimeMillis());
                stats.setSafetyScore(tripScore.getOverallScore());

                dto.setStatistics(stats);

                // 行为统计
                Map<String, Integer> behaviors = tripEvents.stream()
                    .filter(e -> e.getBehavior() != null)
                    .collect(Collectors.groupingBy(
                        EventDataV2::getBehavior,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                    ));
                dto.setBehaviors(behaviors);

                return dto;
            })
            .collect(Collectors.toList());

        DriverTripsResponse response = new DriverTripsResponse();
        response.setDriverId(driver.getDriverId());
        response.setDriverName(driver.getDriverName());
        response.setTotal(total);
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setTrips(tripDTOs);

        return response;
    }

    /**
     * 获取驾驶员安全评分
     */
    public DriverSafetyResponse getDriverSafety(String driverId, Long startTime, Long endTime) {
        Optional<DriverV2> driverOpt = driverRepository.findByDriverId(driverId);
        if (!driverOpt.isPresent()) {
            throw new IllegalArgumentException("驾驶员不存在: " + driverId);
        }

        DriverV2 driver = driverOpt.get();

        // 计算安全评分
        SafetyScoreService.SafetyScoreResult score = safetyScoreService.calculateSafetyScore(driverId, startTime, endTime);

        // 计算趋势（最近30天）
        List<SafetyScoreService.TrendPoint> trendPoints = safetyScoreService.calculateTrend(driverId, 30);

        // 计算排名
        SafetyScoreService.RankingResult ranking = safetyScoreService.calculateRanking(driverId, startTime, endTime);

        // 生成改进建议
        List<SafetyScoreService.Improvement> improvements = safetyScoreService.generateImprovements(driverId, startTime, endTime);

        // 构建响应
        DriverSafetyResponse.ScoreBreakdownDTO breakdown = new DriverSafetyResponse.ScoreBreakdownDTO();
        breakdown.setFatigueScore(score.getFatigueScore());
        breakdown.setBehaviorScore(score.getBehaviorScore());
        breakdown.setComplianceScore(score.getComplianceScore());
        breakdown.setIncidentScore(score.getIncidentScore());

        List<DriverSafetyResponse.TrendPointDTO> trend = trendPoints.stream()
            .map(tp -> {
                DriverSafetyResponse.TrendPointDTO dto = new DriverSafetyResponse.TrendPointDTO();
                dto.setDate(tp.getDate());
                dto.setScore(tp.getScore());
                return dto;
            })
            .collect(Collectors.toList());

        DriverSafetyResponse.RankingsDTO rankings = new DriverSafetyResponse.RankingsDTO();
        rankings.setOverall(ranking.getOverall());
        rankings.setTotalDrivers(ranking.getTotalDrivers());
        rankings.setPercentile(ranking.getPercentile());

        List<DriverSafetyResponse.ImprovementDTO> improvementDTOs = improvements.stream()
            .map(imp -> {
                DriverSafetyResponse.ImprovementDTO dto = new DriverSafetyResponse.ImprovementDTO();
                dto.setType(imp.getType());
                dto.setDescription(imp.getDescription());
                dto.setPriority(imp.getPriority());
                return dto;
            })
            .collect(Collectors.toList());

        DriverSafetyResponse response = new DriverSafetyResponse();
        response.setDriverId(driver.getDriverId());
        response.setDriverName(driver.getDriverName());
        response.setOverallScore(score.getOverallScore());
        response.setScoreBreakdown(breakdown);
        response.setTrend(trend);
        response.setRankings(rankings);
        response.setImprovements(improvementDTOs);

        return response;
    }

    // 辅助方法
    private String getDeviceIdByDriverId(String driverId) {
        List<DeviceDriverBindingV2> bindings = bindingRepository.findByDriverId(driverId);
        Optional<DeviceDriverBindingV2> activeBinding = bindings.stream()
            .filter(b -> b.getStatus() == DeviceDriverBindingV2.BindingStatus.ACTIVE)
            .findFirst();
        return activeBinding.map(DeviceDriverBindingV2::getDeviceId).orElse(null);
    }
}

