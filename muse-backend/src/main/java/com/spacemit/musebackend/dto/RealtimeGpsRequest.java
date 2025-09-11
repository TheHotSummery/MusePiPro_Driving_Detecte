package com.spacemit.musebackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeGpsRequest {
    @NotBlank(message = "原始GPS数据不能为空")
    @JsonProperty("raw_gps_data")
    private String rawGpsData;

    // 疲劳驾驶相关字段
    @JsonProperty("fatigue_score")
    private BigDecimal fatigueScore;

    @JsonProperty("eye_blink_rate")
    private BigDecimal eyeBlinkRate;

    @JsonProperty("head_movement_score")
    private BigDecimal headMovementScore;

    @JsonProperty("yawn_count")
    private Integer yawnCount;

    @JsonProperty("attention_score")
    private BigDecimal attentionScore;
}
