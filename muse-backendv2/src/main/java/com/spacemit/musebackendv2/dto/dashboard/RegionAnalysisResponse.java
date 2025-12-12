package com.spacemit.musebackendv2.dto.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 区域分析响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegionAnalysisResponse implements Serializable {
    
    private String level;  // city/district/road
    
    private List<RegionAnalysisDTO> regions;
    
    private List<TopRegionDTO> topRegions;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegionAnalysisDTO implements Serializable {
        private String regionId;
        private String regionName;
        private String regionType;
        private RegionLocationDTO location;
        private RegionStatisticsDTO statistics;
        private List<HeatmapPointDTO> heatmap;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegionLocationDTO implements Serializable {
        private BigDecimal centerLat;
        private BigDecimal centerLng;
        private MapBoundsDTO bounds;
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
    public static class RegionStatisticsDTO implements Serializable {
        private Integer eventCount;
        private Integer criticalCount;
        private Integer highCount;
        private Integer mediumCount;
        private Integer lowCount;
        private BigDecimal avgScore;
        private String riskLevel;  // high/medium/low
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatmapPointDTO implements Serializable {
        private BigDecimal lat;
        private BigDecimal lng;
        private Double intensity;
        private Integer eventCount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopRegionDTO implements Serializable {
        private String regionName;
        private Integer eventCount;
        private String riskLevel;
    }
}
















