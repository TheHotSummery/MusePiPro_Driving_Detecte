package com.spacemit.musebackendv2.dto.v2;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * V2版本事件数据DTO
 * 对应 dataType: "event" 的 data 字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventDataDTO implements Serializable {
    
    private String eventId;
    
    private String level;  // Normal | Level 1 | Level 2 | Level 3
    
    private BigDecimal score;  // 0-100
    
    private String behavior;
    
    private BigDecimal confidence;  // 0-1
    
    private BigDecimal duration;  // 秒
    
    private BigDecimal locationLat;
    
    private BigDecimal locationLng;
    
    private Integer distractedCount;
}
















