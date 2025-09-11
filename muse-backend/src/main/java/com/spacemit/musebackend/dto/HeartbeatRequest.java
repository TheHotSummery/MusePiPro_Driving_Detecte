package com.spacemit.musebackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeartbeatRequest {
    // deviceId从token中获取，不需要验证
    private String deviceId;

    @NotNull(message = "时间戳不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS][XXX][X][Z]", timezone = "UTC")
    private LocalDateTime timestamp;
}