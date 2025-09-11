package com.spacemit.musebackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {
    @NotBlank(message = "事件ID不能为空")
    private String eventId;

    // deviceId从token中获取，不需要验证
    private String deviceId;

    @NotNull(message = "时间戳不能为空")
    private LocalDateTime timestamp;

    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    @NotBlank(message = "严重程度不能为空")
    private String severity;

    private BigDecimal locationLat;
    private BigDecimal locationLng;
    private String behavior;
    private BigDecimal confidence;
    private BigDecimal duration;
    private String alertLevel;
    private String gpioTriggered; // JSON字符串
    private String context; // JSON字符串
}