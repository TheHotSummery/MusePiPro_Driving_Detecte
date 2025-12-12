package com.spacemit.musebackendv2.dto.v2;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * V2版本GPS数据DTO
 * 对应 dataType: "gps" 的 data 字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GpsDataDTO implements Serializable {
    
    private BigDecimal locationLat;
    
    private BigDecimal locationLng;
    
    private BigDecimal speed;  // km/h
    
    private BigDecimal direction;  // 度
    
    private BigDecimal altitude;  // 米
    
    private Integer satellites;
}
















