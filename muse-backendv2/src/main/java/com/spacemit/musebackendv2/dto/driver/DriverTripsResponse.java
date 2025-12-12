package com.spacemit.musebackendv2.dto.driver;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 驾驶员行程列表响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverTripsResponse implements Serializable {
    
    private String driverId;
    private String driverName;
    private Integer total;
    private Integer page;
    private Integer pageSize;
    private List<TripDTO> trips;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripDTO implements Serializable {
        private String tripId;
        private String deviceId;
        private Long startTime;
        private Long endTime;
        private Integer duration;
        private BigDecimal distance;
        private LocationDTO startLocation;
        private LocationDTO endLocation;
        private TripStatisticsDTO statistics;
        private Map<String, Integer> behaviors;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationDTO implements Serializable {
        private BigDecimal lat;
        private BigDecimal lng;
        private String address;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripStatisticsDTO implements Serializable {
        private Integer eventCount;
        private Integer criticalEvents;
        private Integer highEvents;
        private Integer mediumEvents;
        private Integer lowEvents;
        private BigDecimal maxScore;
        private BigDecimal avgScore;
        private BigDecimal safetyScore;
    }
}
















