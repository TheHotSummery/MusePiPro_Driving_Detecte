package com.spacemit.musebackendv2.dto.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 告警事件DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertDTO implements Serializable {
    
    private String alertId;
    
    private String deviceId;
    
    private String driverId;
    
    private String driverName;
    
    private String level;  // Level 1/Level 2/Level 3
    
    private BigDecimal score;
    
    private String behavior;
    
    private LocationDTO location;
    
    private Long timestamp;
    
    private BigDecimal duration;  // 持续时间（秒）
    
    private String status;  // active/resolved
}
















