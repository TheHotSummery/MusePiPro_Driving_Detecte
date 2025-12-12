package com.spacemit.musebackendv2.dto.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 疲劳事件统计响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventStatisticsResponse implements Serializable {
    
    private SummaryDTO summary;
    
    private Map<String, Long> byLevel;  // Level 3/Level 2/Level 1/Normal
    
    private Map<String, Long> byType;  // FATIGUE/DISTRACTION/EMERGENCY
    
    private Map<String, Long> byBehavior;  // eyes_closed/yarning/head_down等
    
    private TrendDTO trend;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryDTO implements Serializable {
        private Long totalEvents;
        private Long criticalEvents;
        private Long highEvents;
        private Long mediumEvents;
        private Long lowEvents;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendDTO implements Serializable {
        private Long today;
        private Long yesterday;
        private Long thisWeek;
        private Long lastWeek;
        private Long thisMonth;
        private Long lastMonth;
    }
}
















