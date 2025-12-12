package com.spacemit.musebackendv2.dto.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 区域分布图响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegionDistributionResponse implements Serializable {
    
    private String level;  // province/city/district
    
    private List<RegionDataDTO> data;
    
    private List<TopRegionDTO> topRegions;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegionDataDTO implements Serializable {
        private String regionName;
        private String regionType;
        private Integer count;
        private Integer criticalCount;
        private Integer highCount;
        private Integer mediumCount;
        private Integer lowCount;
        private Double avgScore;
        private Double percentage;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopRegionDTO implements Serializable {
        private String regionName;
        private Integer eventCount;
        private String riskLevel;  // high/medium/low
    }
}
















