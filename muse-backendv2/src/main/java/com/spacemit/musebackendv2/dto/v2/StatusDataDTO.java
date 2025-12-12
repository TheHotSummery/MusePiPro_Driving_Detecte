package com.spacemit.musebackendv2.dto.v2;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * V2版本状态数据DTO
 * 对应 dataType: "status" 的 data 字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusDataDTO implements Serializable {
    
    private String level;  // Normal | Level 1 | Level 2 | Level 3
    
    private BigDecimal score;  // 0-100
    
    private BigDecimal locationLat;
    
    private BigDecimal locationLng;
    
    private BigDecimal cpuUsage;  // %
    
    private BigDecimal memoryUsage;  // %
    
    private BigDecimal temperature;  // ℃
}
















