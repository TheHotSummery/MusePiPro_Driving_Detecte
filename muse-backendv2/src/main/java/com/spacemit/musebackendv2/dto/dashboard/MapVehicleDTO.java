package com.spacemit.musebackendv2.dto.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * 地图车辆DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapVehicleDTO implements Serializable {
    
    private String deviceId;
    
    private String driverId;
    
    private String driverName;
    
    private LocationDTO location;
    
    private VehicleStatusDTO status;
    
    private Long lastUpdateTime;
}
















