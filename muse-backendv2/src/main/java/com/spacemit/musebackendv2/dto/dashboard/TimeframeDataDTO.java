package com.spacemit.musebackendv2.dto.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 时间段数据DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeframeDataDTO implements Serializable {
    
    private String time;  // 时间字符串，如 "2025-01-15 00:00:00"
    
    private Long timestamp;  // 毫秒时间戳
    
    private Long eventCount;
    
    private Long criticalCount;
    
    private Long highCount;
    
    private Long mediumCount;
    
    private Long lowCount;
    
    private BigDecimal avgScore;
    
    private BigDecimal maxScore;
}
















