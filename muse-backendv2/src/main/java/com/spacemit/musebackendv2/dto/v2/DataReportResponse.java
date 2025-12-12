package com.spacemit.musebackendv2.dto.v2;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * V2版本统一数据上报响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataReportResponse implements Serializable {
    
    private Boolean received;
    
    private Long serverTime;
}
















