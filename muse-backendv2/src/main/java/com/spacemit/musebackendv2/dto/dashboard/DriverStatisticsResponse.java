package com.spacemit.musebackendv2.dto.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 驾驶员统计响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverStatisticsResponse implements Serializable {
    
    private Integer totalDrivers;
    
    private Integer activeDrivers;
    
    private List<DriverStatisticsDTO> drivers;
    
    private List<TopDriverDTO> topDrivers;
    
    private List<RiskDriverDTO> riskDrivers;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverStatisticsDTO implements Serializable {
        private String driverId;
        private String driverName;
        private String phone;
        private String licenseNumber;
        private String avatar;
        private String teamName;
        private DriverStatsDTO statistics;
        private List<RecentTripDTO> recentTrips;
        private Map<String, Integer> behaviorStats;
        private TimeDistributionDTO timeDistribution;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverStatsDTO implements Serializable {
        private Integer totalTrips;
        private BigDecimal totalDistance;
        private Integer totalDuration;
        private Integer totalEvents;
        private Integer criticalEvents;
        private Integer highEvents;
        private Integer mediumEvents;
        private Integer lowEvents;
        private BigDecimal avgScore;
        private BigDecimal maxScore;
        private BigDecimal safetyScore;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentTripDTO implements Serializable {
        private String tripId;
        private Long startTime;
        private Long endTime;
        private BigDecimal distance;
        private Integer duration;
        private Integer eventCount;
        private String maxLevel;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeDistributionDTO implements Serializable {
        private Integer morning;  // 6-12
        private Integer afternoon;  // 12-18
        private Integer evening;  // 18-24
        private Integer night;  // 0-6
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopDriverDTO implements Serializable {
        private String driverId;
        private String driverName;
        private BigDecimal safetyScore;
        private Integer totalTrips;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskDriverDTO implements Serializable {
        private String driverId;
        private String driverName;
        private BigDecimal safetyScore;
        private Integer criticalEvents;
    }
}
















