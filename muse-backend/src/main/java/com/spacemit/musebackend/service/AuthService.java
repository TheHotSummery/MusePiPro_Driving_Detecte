package com.spacemit.musebackend.service;

import com.spacemit.musebackend.dto.LoginRequest;
import com.spacemit.musebackend.dto.LoginResponse;
import com.spacemit.musebackend.entity.Device;
import com.spacemit.musebackend.entity.User;
import com.spacemit.musebackend.repository.DeviceRepository;
import com.spacemit.musebackend.repository.UserRepository;
import com.spacemit.musebackend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        try {
            // 查找或创建设备
            Device device = findOrCreateDevice(request);
            
            // 查找用户（如果有用户名密码）
            User user = null;
            if (request.getUsername() != null && request.getPassword() != null) {
                user = authenticateUser(request.getUsername(), request.getPassword());
            }
            
            // 生成Token
            String token = jwtUtil.generateToken(device.getDeviceId(), user != null ? user.getId() : null);
            
            // 更新设备状态
            device.setStatus(Device.DeviceStatus.ONLINE);
            device.setLastSeen(LocalDateTime.now());
            deviceRepository.save(device);
            
            // 构建响应
            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setRefreshToken(token); // 简化处理，实际应该生成不同的refresh token
            response.setExpiresIn(86400L); // 24小时
            
            if (user != null) {
                LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
                userInfo.setUserId(user.getId());
                userInfo.setUsername(user.getUsername());
                userInfo.setEmail(user.getEmail());
                userInfo.setPhone(user.getPhone());
                response.setUser(userInfo);
            }
            
            return response;
        } catch (Exception e) {

            log.error("登录过程中发生错误: {}", e.getMessage(), e);
            throw new RuntimeException("登录失败: " + e.getMessage());
        }
    }

    private Device findOrCreateDevice(LoginRequest request) {
        Optional<Device> deviceOpt = deviceRepository.findByDeviceId(request.getDeviceId());
        if (deviceOpt.isPresent()) {
            return deviceOpt.get();
        } else {
            Device device = new Device();
            device.setDeviceId(request.getDeviceId());
            device.setDeviceType(request.getDeviceType());
            device.setVersion(request.getVersion());
            device.setStatus(Device.DeviceStatus.ONLINE);
            device.setLastSeen(LocalDateTime.now());
            return deviceRepository.save(device);
        }
    }

    private User authenticateUser(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // 简化密码验证，实际应该使用BCrypt
            if (user.getPasswordHash().equals(password)) {
                return user;
            }
        }
        return null;
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

    public Integer getUserIdFromToken(String token) {
        return jwtUtil.getUserIdFromToken(token);
    }
}