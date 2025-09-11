package com.spacemit.musebackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    @NotBlank(message = "设备类型不能为空")
    private String deviceType;

    @NotBlank(message = "版本号不能为空")
    private String version;

    private String username;
    private String password;
}