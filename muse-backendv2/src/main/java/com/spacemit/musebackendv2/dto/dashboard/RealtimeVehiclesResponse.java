package com.spacemit.musebackendv2.dto.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 实时车辆状态响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeVehiclesResponse implements Serializable {
    
    private Integer totalVehicles;
    
    private Integer onlineVehicles;
    
    private Integer offlineVehicles;
    
    private List<VehicleStatusDTO> vehicles;
}
















