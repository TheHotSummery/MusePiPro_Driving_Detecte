package com.spacemit.musebackendv2.dto.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 车辆轨迹回放响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackResponse implements Serializable {
    
    private String deviceId;
    
    private String driverId;
    
    private String driverName;
    
    private String tripId;
    
    private Long startTime;
    
    private Long endTime;
    
    private BigDecimal totalDistance;  // 公里
    
    private Integer totalDuration;  // 秒
    
    private List<TrackPointDTO> track;
    
    private List<AlertDTO> events;
    
    private TrackStatisticsDTO statistics;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrackStatisticsDTO implements Serializable {
        private Integer totalEvents;
        private Integer criticalEvents;
        private Integer highEvents;
        private Integer mediumEvents;
        private Integer lowEvents;
        private BigDecimal avgScore;
        private BigDecimal maxScore;
    }
}
















