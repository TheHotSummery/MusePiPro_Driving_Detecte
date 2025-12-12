package com.spacemit.musebackendv2.dto.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * 车辆状态DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleStatusDTO implements Serializable {
    
    private String deviceId;
    
    private String driverId;
    
    private String driverName;
    
    private String status;  // online/offline/error
    
    private String currentLevel;  // Normal/Level 1/Level 2/Level 3
    
    private Double currentScore;
    
    private LocationDTO location;
    
    private Long lastUpdateTime;
    
    private Long uptime;  // 运行时长（秒）
}
















