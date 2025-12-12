package com.spacemit.musebackendv2.dto.driver;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 驾驶员基本信息响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverInfoResponse implements Serializable {
    
    private String driverId;
    private String driverName;
    private String phone;
    private String email;
    private String licenseNumber;
    private String licenseType;
    private String licenseExpire;
    private String avatar;
    private String teamName;
    private List<BindDeviceDTO> bindDevices;
    private DriverStatisticsDTO statistics;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BindDeviceDTO implements Serializable {
        private String deviceId;
        private String deviceName;
        private Long bindTime;
        private String status; // active/inactive
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverStatisticsDTO implements Serializable {
        private Integer totalTrips;
        private BigDecimal totalDistance;
        private Integer totalDuration;
        private Integer totalEvents;
        private BigDecimal safetyScore;
        private String joinDate;
    }
}
















