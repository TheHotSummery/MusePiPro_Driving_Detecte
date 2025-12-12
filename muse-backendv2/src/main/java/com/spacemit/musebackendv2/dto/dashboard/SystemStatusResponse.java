package com.spacemit.musebackendv2.dto.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 系统运行状态响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatusResponse implements Serializable {
    
    private Integer totalDevices;
    
    private Integer healthyDevices;
    
    private Integer warningDevices;
    
    private Integer errorDevices;
    
    private SystemStatsDTO systemStats;
    
    private List<DeviceStatusDTO> deviceStatus;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemStatsDTO implements Serializable {
        private Double avgCpuUsage;
        private Double avgMemoryUsage;
        private Double avgTemperature;
        private Integer avgNetworkLatency;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceStatusDTO implements Serializable {
        private String deviceId;
        private String status;  // healthy/warning/error
        private Double cpuUsage;
        private Double memoryUsage;
        private Double temperature;
        private String networkStatus;  // online/offline
        private Long lastHeartbeat;
    }
}
















