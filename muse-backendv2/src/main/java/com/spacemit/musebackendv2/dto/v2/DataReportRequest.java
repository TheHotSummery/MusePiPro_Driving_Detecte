package com.spacemit.musebackendv2.dto.v2;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * V2版本统一数据上报请求DTO
 * 对应硬件上报接口：POST /api/v2/data/report
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataReportRequest implements Serializable {
    
    @NotBlank(message = "数据类型不能为空")
    private String dataType;  // event | status | gps
    
    @NotNull(message = "时间戳不能为空")
    private Long timestamp;   // 毫秒时间戳
    
    @NotNull(message = "数据内容不能为空")
    private Object data;      // 根据dataType不同，data结构不同
}
















