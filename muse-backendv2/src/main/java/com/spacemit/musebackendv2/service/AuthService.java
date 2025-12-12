package com.spacemit.musebackendv2.service;

import com.spacemit.musebackendv2.dto.TokenResponse;
import com.spacemit.musebackendv2.entity.v2.DeviceV2;
import com.spacemit.musebackendv2.repository.v2.DeviceV2Repository;
import com.spacemit.musebackendv2.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final DeviceV2Repository deviceRepository;
    private final JwtUtil jwtUtil;

    /**
     * 为设备获取Token
     * 如果设备不存在，自动创建
     */
    public TokenResponse getTokenForDevice(String deviceId) {
        try {
            // 查找或创建设备
            DeviceV2 device = findOrCreateDeviceByDeviceId(deviceId);
            
            // 生成Token
            String token = jwtUtil.generateToken(device.getDeviceId(), null);
            
            // 更新设备状态
            device.setStatus(DeviceV2.DeviceStatus.ONLINE);
            device.setLastReportTime(System.currentTimeMillis());
            device.setUpdatedAt(System.currentTimeMillis());
            deviceRepository.save(device);
            
            // 构建响应
            TokenResponse response = new TokenResponse();
            response.setToken(token);
            response.setExpiresIn(86400L); // 24小时
            response.setDeviceId(deviceId);
            
            log.info("Token获取成功: deviceId={}", deviceId);
            return response;
        } catch (Exception e) {
            log.error("Token获取失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
            throw new RuntimeException("Token获取失败: " + e.getMessage());
        }
    }

    /**
     * 查找或创建设备
     */
    public DeviceV2 findOrCreateDeviceByDeviceId(String deviceId) {
        Optional<DeviceV2> deviceOpt = deviceRepository.findByDeviceId(deviceId);
        if (deviceOpt.isPresent()) {
            return deviceOpt.get();
        } else {
            DeviceV2 device = new DeviceV2();
            device.setDeviceId(deviceId);
            device.setStatus(DeviceV2.DeviceStatus.ONLINE);
            long now = System.currentTimeMillis();
            device.setFirstReportTime(now);
            device.setLastReportTime(now);
            device.setCreatedAt(now);
            device.setUpdatedAt(now);
            DeviceV2 saved = deviceRepository.save(device);
            log.info("自动创建新设备: deviceId={}", deviceId);
            return saved;
        }
    }

    public boolean validateToken(String token) {
        try {
            String deviceId = jwtUtil.getUsernameFromToken(token);
            return jwtUtil.validateToken(token, deviceId);
        } catch (Exception e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    public String getDeviceIdFromToken(String token) {
        return jwtUtil.getUsernameFromToken(token);
    }
}
















