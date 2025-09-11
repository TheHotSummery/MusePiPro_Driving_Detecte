package com.spacemit.musebackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class RealtimeDataRequest {
    // deviceId从token中获取，不需要验证
    @JsonProperty("deviceId")
    private String deviceId;

    @NotNull(message = "时间戳不能为空")
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("location_lat")
    private BigDecimal locationLat;
    
    @JsonProperty("location_lng")
    private BigDecimal locationLng;
    
    @JsonProperty("speed")
    private BigDecimal speed;
    
    @JsonProperty("direction")
    private BigDecimal direction;
    
    @JsonProperty("altitude")
    private BigDecimal altitude;
    
    @JsonProperty("hdop")
    private BigDecimal hdop;
    
    @JsonProperty("satellites")
    private Integer satellites;
    
    @JsonProperty("fix_mode")
    private Integer fixMode;
}