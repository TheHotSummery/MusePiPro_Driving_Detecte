package com.spacemit.musebackendv2.dto.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 行为类型分布响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BehaviorDistributionResponse implements Serializable {
    
    private List<BehaviorDataDTO> data;
    
    private BehaviorStatisticsDTO statistics;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BehaviorDataDTO implements Serializable {
        private String behavior;
        private String behaviorName;  // 中文名称
        private Integer count;
        private Integer criticalCount;
        private Integer highCount;
        private Integer mediumCount;
        private Integer lowCount;
        private Double avgScore;
        private Double percentage;  // 占比
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BehaviorStatisticsDTO implements Serializable {
        private Integer totalBehaviors;
        private Integer totalEvents;
        private String topBehavior;
        private Double topBehaviorPercentage;
    }
}
















