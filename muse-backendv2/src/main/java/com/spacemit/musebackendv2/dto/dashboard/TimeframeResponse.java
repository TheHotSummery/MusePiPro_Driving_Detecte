package com.spacemit.musebackendv2.dto.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 时间段分析响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeframeResponse implements Serializable {
    
    private String interval;  // hour/day/week/month
    
    private List<TimeframeDataDTO> data;
    
    private List<PeakHourDTO> peakHours;
    
    private StatisticsDTO statistics;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeakHourDTO implements Serializable {
        private Integer hour;
        private Long eventCount;
        private BigDecimal avgScore;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatisticsDTO implements Serializable {
        private Long totalEvents;
        private BigDecimal avgEventsPerHour;
        private Long maxEventsInHour;
        private Long minEventsInHour;
    }
}

