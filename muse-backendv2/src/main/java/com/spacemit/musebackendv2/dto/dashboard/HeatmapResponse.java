package com.spacemit.musebackendv2.dto.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 热力图响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapResponse implements Serializable {
    
    private String level;  // 筛选的告警级别（可选）
    
    private Long startTime;
    
    private Long endTime;
    
    private List<HeatmapPointDTO> points;
    
    private MapBoundsDTO bounds;
    
    private HeatmapStatisticsDTO statistics;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatmapPointDTO implements Serializable {
        private BigDecimal lat;
        private BigDecimal lng;
        private Double intensity;  // 强度（0-1）
        private Integer eventCount;
        private BigDecimal maxScore;
        private BigDecimal avgScore;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MapBoundsDTO implements Serializable {
        private Double north;
        private Double south;
        private Double east;
        private Double west;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatmapStatisticsDTO implements Serializable {
        private Integer totalPoints;
        private Double maxIntensity;
        private Double minIntensity;
        private Integer totalEvents;
    }
}
















