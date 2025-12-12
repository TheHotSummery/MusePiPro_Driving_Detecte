package com.spacemit.musebackendv2.dto.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 实时告警事件响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeAlertsResponse implements Serializable {
    
    private Integer activeAlerts;
    
    private Integer criticalAlerts;  // Level 3
    
    private Integer highAlerts;  // Level 2
    
    private Integer mediumAlerts;  // Level 1
    
    private List<AlertDTO> alerts;
}
















