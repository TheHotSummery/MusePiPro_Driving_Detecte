package com.spacemit.musebackendv2.dto.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 时间段分布图响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeDistributionResponse implements Serializable {
    
    private String groupBy;  // hour/day/week/month
    
    private List<DistributionDataDTO> data;
    
    private List<PeakPeriodDTO> peakPeriods;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistributionDataDTO implements Serializable {
        private String label;
        private Integer value;
        private Integer criticalCount;
        private Integer highCount;
        private Integer mediumCount;
        private Integer lowCount;
        private Double avgScore;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeakPeriodDTO implements Serializable {
        private Integer start;
        private Integer end;
        private String label;
        private Integer eventCount;
        private Double avgScore;
    }
}
















