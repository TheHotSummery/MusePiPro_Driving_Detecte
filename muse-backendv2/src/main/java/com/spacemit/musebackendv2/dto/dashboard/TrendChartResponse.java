package com.spacemit.musebackendv2.dto.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 疲劳趋势曲线响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendChartResponse implements Serializable {
    
    private String deviceId;
    
    private String driverId;
    
    private String driverName;
    
    private String interval;  // minute/hour/day
    
    private List<ChartSeriesDTO> series;
    
    private TrendStatisticsDTO statistics;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartSeriesDTO implements Serializable {
        private String name;
        private String type;  // line/bar
        private List<ChartDataPointDTO> data;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartDataPointDTO implements Serializable {
        private String time;
        private Long timestamp;
        private Double value;
        private String level;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendStatisticsDTO implements Serializable {
        private Double minScore;
        private Double maxScore;
        private Double avgScore;
        private Integer totalEvents;
    }
}
















