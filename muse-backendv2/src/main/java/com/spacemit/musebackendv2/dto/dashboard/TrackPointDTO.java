package com.spacemit.musebackendv2.dto.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 轨迹点DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackPointDTO implements Serializable {
    
    private Long timestamp;
    
    private LocationDTO location;
    
    private FatigueDTO fatigue;
    
    private List<EventMarkerDTO> events;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FatigueDTO implements Serializable {
        private Double score;
        private String level;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventMarkerDTO implements Serializable {
        private String eventId;
        private String level;
        private String behavior;
        private Long timestamp;
        private java.math.BigDecimal score;  // 事件分数
        private String address;  // 事件地址
    }
}

