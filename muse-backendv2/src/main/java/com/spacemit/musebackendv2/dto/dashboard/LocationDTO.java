package com.spacemit.musebackendv2.dto.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 位置信息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationDTO implements Serializable {
    
    private BigDecimal lat;
    
    private BigDecimal lng;
    
    private BigDecimal speed;  // km/h
    
    private BigDecimal heading;  // 方向角（度）
    
    private BigDecimal altitude;  // 海拔（米）
    
    private String address;  // 地址（可选）
}
















