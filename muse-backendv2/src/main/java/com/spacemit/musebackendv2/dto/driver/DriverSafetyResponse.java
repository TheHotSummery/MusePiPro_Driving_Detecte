package com.spacemit.musebackendv2.dto.driver;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 驾驶员安全评分响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverSafetyResponse implements Serializable {
    
    private String driverId;
    private String driverName;
    private BigDecimal overallScore;
    private ScoreBreakdownDTO scoreBreakdown;
    private List<TrendPointDTO> trend;
    private RankingsDTO rankings;
    private List<ImprovementDTO> improvements;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreBreakdownDTO implements Serializable {
        private BigDecimal fatigueScore;
        private BigDecimal behaviorScore;
        private BigDecimal complianceScore;
        private BigDecimal incidentScore;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPointDTO implements Serializable {
        private String date;
        private BigDecimal score;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankingsDTO implements Serializable {
        private Integer overall;
        private Integer totalDrivers;
        private BigDecimal percentile;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImprovementDTO implements Serializable {
        private String type;
        private String description;
        private String priority; // high/medium/low
    }
}
















