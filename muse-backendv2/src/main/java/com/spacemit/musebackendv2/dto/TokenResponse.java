package com.spacemit.musebackendv2.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * Token获取响应DTO
 * 对应规范：POST /api/v2/auth/token
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse implements Serializable {
    
    private String token;
    
    private Long expiresIn;  // Token有效期（秒）
    
    private String deviceId;
}
















