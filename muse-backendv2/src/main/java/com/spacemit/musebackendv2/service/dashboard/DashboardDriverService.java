package com.spacemit.musebackendv2.service.dashboard;

import com.spacemit.musebackendv2.dto.dashboard.DriverStatisticsResponse;
import com.spacemit.musebackendv2.entity.v2.*;
import com.spacemit.musebackendv2.repository.v2.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 驾驶员统计服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardDriverService {

    private final DriverV2Repository driverRepository;
    private final TeamV2Repository teamRepository;
    private final EventDataV2Repository eventDataRepository;
    private final TripV2Repository tripRepository;
    private final DeviceDriverBindingV2Repository bindingRepository;
    private final GpsDataV2Repository gpsDataRepository;

    /**
     * 获取驾驶员统计
     */
    public DriverStatisticsResponse getDriverStatistics(Long startTime, Long endTime) {
        // 获取所有驾驶员（包括非活跃的）
        List<DriverV2> allDrivers = driverRepository.findAll();
        
        int totalDrivers = allDrivers.size();
        int activeDrivers = 0;
        
        List<DriverStatisticsResponse.DriverStatisticsDTO> drivers = new ArrayList<>();
        
        for (DriverV2 driver : allDrivers) {
            // 获取该驾驶员在该时间段内的事件
            List<EventDataV2> events = eventDataRepository.findByDriverIdAndTimestampBetween(
                driver.getDriverId(), startTime, endTime);
            
            // 获取该驾驶员的行程
            List<TripV2> trips = tripRepository.findByDriverIdAndStartTimeBetween(
                driver.getDriverId(), startTime, endTime);
            
            // 判断是否活跃（有事件或行程）
            boolean isActive = !events.isEmpty() || !trips.isEmpty();
            if (isActive) {
                activeDrivers++;
            }
            
            // 计算统计数据
            int totalTrips = trips.size();
            BigDecimal totalDistance = trips.stream()
                .filter(t -> t.getTotalDistance() != null)
                .map(TripV2::getTotalDistance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            int totalDuration = trips.stream()
                .filter(t -> t.getTotalDuration() != null)
                .mapToInt(TripV2::getTotalDuration)
                .sum();
            
            int totalEvents = events.size();
            int criticalEvents = (int) events.stream().filter(e -> "Level 3".equals(e.getLevel())).count();
            int highEvents = (int) events.stream().filter(e -> "Level 2".equals(e.getLevel())).count();
            int mediumEvents = (int) events.stream().filter(e -> "Level 1".equals(e.getLevel())).count();
            int lowEvents = (int) events.stream().filter(e -> "Normal".equals(e.getLevel())).count();
            
            OptionalDouble avgScoreOpt = events.stream()
                .filter(e -> e.getScore() != null)
                .mapToDouble(e -> e.getScore().doubleValue())
                .average();
            BigDecimal avgScore = avgScoreOpt.isPresent() ? 
                BigDecimal.valueOf(avgScoreOpt.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
            
            OptionalDouble maxScoreOpt = events.stream()
                .filter(e -> e.getScore() != null)
                .mapToDouble(e -> e.getScore().doubleValue())
                .max();
            BigDecimal maxScore = maxScoreOpt.isPresent() ? 
                BigDecimal.valueOf(maxScoreOpt.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
            
            // 计算安全评分（简化算法）
            // 基础分100，根据事件数量和严重程度扣分
            BigDecimal safetyScore = calculateSafetyScore(totalEvents, criticalEvents, highEvents, mediumEvents, avgScore);
            
            // 获取车队名称
            String teamName = null;
            if (driver.getTeamId() != null) {
                Optional<TeamV2> teamOpt = teamRepository.findByTeamId(driver.getTeamId());
                if (teamOpt.isPresent()) {
                    teamName = teamOpt.get().getTeamName();
                }
            }
            
            // 最近行程（最多5个）
            List<DriverStatisticsResponse.RecentTripDTO> recentTrips = trips.stream()
                .sorted((a, b) -> Long.compare(b.getStartTime(), a.getStartTime()))
                .limit(5)
                .map(t -> {
                    DriverStatisticsResponse.RecentTripDTO trip = new DriverStatisticsResponse.RecentTripDTO();
                    trip.setTripId(t.getTripId());
                    trip.setStartTime(t.getStartTime());
                    trip.setEndTime(t.getEndTime());
                    trip.setDistance(t.getTotalDistance() != null ? t.getTotalDistance() : BigDecimal.ZERO);
                    trip.setDuration(t.getTotalDuration() != null ? t.getTotalDuration() : 0);
                    trip.setEventCount(t.getEventCount() != null ? t.getEventCount() : 0);
                    trip.setMaxLevel(t.getMaxLevel());
                    return trip;
                })
                .collect(Collectors.toList());
            
            // 行为统计
            Map<String, Integer> behaviorStats = events.stream()
                .filter(e -> e.getBehavior() != null)
                .collect(Collectors.groupingBy(
                    EventDataV2::getBehavior,
                    Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
            
            // 时间段分布
            DriverStatisticsResponse.TimeDistributionDTO timeDistribution = calculateTimeDistribution(events);
            
            DriverStatisticsResponse.DriverStatsDTO stats = new DriverStatisticsResponse.DriverStatsDTO();
            stats.setTotalTrips(totalTrips);
            stats.setTotalDistance(totalDistance);
            stats.setTotalDuration(totalDuration);
            stats.setTotalEvents(totalEvents);
            stats.setCriticalEvents(criticalEvents);
            stats.setHighEvents(highEvents);
            stats.setMediumEvents(mediumEvents);
            stats.setLowEvents(lowEvents);
            stats.setAvgScore(avgScore);
            stats.setMaxScore(maxScore);
            stats.setSafetyScore(safetyScore);
            
            DriverStatisticsResponse.DriverStatisticsDTO driverStats = new DriverStatisticsResponse.DriverStatisticsDTO();
            driverStats.setDriverId(driver.getDriverId());
            driverStats.setDriverName(driver.getDriverName());
            driverStats.setPhone(driver.getPhone());
            driverStats.setLicenseNumber(driver.getLicenseNumber());
            driverStats.setAvatar(driver.getAvatarUrl());
            driverStats.setTeamName(teamName);
            driverStats.setStatistics(stats);
            driverStats.setRecentTrips(recentTrips);
            driverStats.setBehaviorStats(behaviorStats);
            driverStats.setTimeDistribution(timeDistribution);
            
            drivers.add(driverStats);
        }
        
        // 按安全评分排序
        drivers.sort((a, b) -> {
            BigDecimal scoreA = a.getStatistics().getSafetyScore();
            BigDecimal scoreB = b.getStatistics().getSafetyScore();
            return scoreB.compareTo(scoreA);
        });
        
        // 优秀驾驶员排行（安全评分前10）
        List<DriverStatisticsResponse.TopDriverDTO> topDrivers = drivers.stream()
            .limit(10)
            .map(d -> {
                DriverStatisticsResponse.TopDriverDTO top = new DriverStatisticsResponse.TopDriverDTO();
                top.setDriverId(d.getDriverId());
                top.setDriverName(d.getDriverName());
                top.setSafetyScore(d.getStatistics().getSafetyScore());
                top.setTotalTrips(d.getStatistics().getTotalTrips());
                return top;
            })
            .collect(Collectors.toList());
        
        // 高风险驾驶员（安全评分低于60或严重事件数>5）
        List<DriverStatisticsResponse.RiskDriverDTO> riskDrivers = drivers.stream()
            .filter(d -> {
                BigDecimal score = d.getStatistics().getSafetyScore();
                int critical = d.getStatistics().getCriticalEvents();
                return score.compareTo(BigDecimal.valueOf(60)) < 0 || critical > 5;
            })
            .map(d -> {
                DriverStatisticsResponse.RiskDriverDTO risk = new DriverStatisticsResponse.RiskDriverDTO();
                risk.setDriverId(d.getDriverId());
                risk.setDriverName(d.getDriverName());
                risk.setSafetyScore(d.getStatistics().getSafetyScore());
                risk.setCriticalEvents(d.getStatistics().getCriticalEvents());
                return risk;
            })
            .collect(Collectors.toList());
        
        DriverStatisticsResponse response = new DriverStatisticsResponse();
        response.setTotalDrivers(totalDrivers);
        response.setActiveDrivers(activeDrivers);
        response.setDrivers(drivers);
        response.setTopDrivers(topDrivers);
        response.setRiskDrivers(riskDrivers);
        
        return response;
    }

    /**
     * 计算安全评分
     * 简化算法：基础分100，根据事件扣分
     */
    private BigDecimal calculateSafetyScore(int totalEvents, int criticalEvents, int highEvents, 
                                            int mediumEvents, BigDecimal avgScore) {
        // 基础分
        double score = 100.0;
        
        // 根据事件数量扣分
        score -= totalEvents * 0.5;  // 每个事件扣0.5分
        
        // 根据严重程度扣分
        score -= criticalEvents * 5.0;  // 严重事件每个扣5分
        score -= highEvents * 2.0;      // 高级别事件每个扣2分
        score -= mediumEvents * 1.0;    // 中级别事件每个扣1分
        
        // 根据平均分数调整（如果平均分数高，说明风险高，需要扣分）
        if (avgScore.compareTo(BigDecimal.ZERO) > 0) {
            double avgScoreValue = avgScore.doubleValue();
            if (avgScoreValue > 80) {
                score -= (avgScoreValue - 80) * 0.5;  // 超过80的部分，每分扣0.5
            }
        }
        
        // 确保分数在0-100之间
        score = Math.max(0, Math.min(100, score));
        
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算时间段分布
     */
    private DriverStatisticsResponse.TimeDistributionDTO calculateTimeDistribution(List<EventDataV2> events) {
        int morning = 0;    // 6-12
        int afternoon = 0;  // 12-18
        int evening = 0;    // 18-24
        int night = 0;      // 0-6
        
        Calendar cal = Calendar.getInstance();
        for (EventDataV2 event : events) {
            cal.setTimeInMillis(event.getTimestamp());
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            
            if (hour >= 6 && hour < 12) {
                morning++;
            } else if (hour >= 12 && hour < 18) {
                afternoon++;
            } else if (hour >= 18 && hour < 24) {
                evening++;
            } else {
                night++;
            }
        }
        
        DriverStatisticsResponse.TimeDistributionDTO timeDist = new DriverStatisticsResponse.TimeDistributionDTO();
        timeDist.setMorning(morning);
        timeDist.setAfternoon(afternoon);
        timeDist.setEvening(evening);
        timeDist.setNight(night);
        
        return timeDist;
    }
}

