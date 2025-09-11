package com.spacemit.musebackend.service;

import com.spacemit.musebackend.dto.CreateUserRequest;
import com.spacemit.musebackend.dto.UpdateUserRequest;
import com.spacemit.musebackend.dto.UserResponse;
import com.spacemit.musebackend.entity.Device;
import com.spacemit.musebackend.entity.User;
import com.spacemit.musebackend.repository.DeviceRepository;
import com.spacemit.musebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;

    /**
     * 获取用户列表
     */
    public Map<String, Object> getUsers(int page, int size, String keyword, String status) {
        Pageable pageable = PageRequest.of(page, size);
        User.UserStatus statusEnum = null;
        
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusEnum = User.UserStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("无效的用户状态: {}", status);
            }
        }
        
        Page<User> userPage = userRepository.findUsersWithFilters(keyword, statusEnum, pageable);
        
        List<UserResponse> users = userPage.getContent().stream().map(user -> {
            UserResponse response = UserResponse.fromUser(user);
            
            // 统计设备数量
            List<Device> devices = deviceRepository.findByUserId(user.getId());
            response.setDeviceCount(devices.size());
            
            return response;
        }).collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", userPage.getTotalElements());
        result.put("users", users);
        
        return result;
    }

    /**
     * 获取用户详情
     */
    public UserResponse getUserById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));
        
        UserResponse response = UserResponse.fromUser(user);
        
        // 获取用户设备列表
        List<Device> devices = deviceRepository.findByUserId(userId);
        List<UserResponse.DeviceInfo> deviceInfos = devices.stream().map(device -> {
            UserResponse.DeviceInfo deviceInfo = new UserResponse.DeviceInfo();
            deviceInfo.setDeviceId(device.getDeviceId());
            deviceInfo.setDeviceType(device.getDeviceType());
            deviceInfo.setStatus(device.getStatus().name());
            return deviceInfo;
        }).collect(Collectors.toList());
        
        response.setDevices(deviceInfos);
        response.setDeviceCount(devices.size());
        
        return response;
    }

    /**
     * 创建用户
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在: " + request.getUsername());
        }
        
        // 检查邮箱是否已存在
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty() 
            && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已存在: " + request.getEmail());
        }
        
        // 检查手机号是否已存在
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty() 
            && userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("手机号已存在: " + request.getPhone());
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(request.getPassword()); // 实际应该使用BCrypt加密
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(User.UserStatus.valueOf(request.getStatus()));
        
        User savedUser = userRepository.save(user);
        log.info("用户创建成功: userId={}, username={}", savedUser.getId(), savedUser.getUsername());
        
        return UserResponse.fromUser(savedUser);
    }

    /**
     * 更新用户
     */
    @Transactional
    public UserResponse updateUser(Integer userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));
        
        // 检查用户名是否已被其他用户使用
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("用户名已存在: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }
        
        // 检查邮箱是否已被其他用户使用
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("邮箱已存在: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        
        // 检查手机号是否已被其他用户使用
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new RuntimeException("手机号已存在: " + request.getPhone());
            }
            user.setPhone(request.getPhone());
        }
        
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPasswordHash(request.getPassword()); // 实际应该使用BCrypt加密
        }
        
        if (request.getStatus() != null) {
            user.setStatus(User.UserStatus.valueOf(request.getStatus()));
        }
        
        User savedUser = userRepository.save(user);
        log.info("用户更新成功: userId={}, username={}", savedUser.getId(), savedUser.getUsername());
        
        return UserResponse.fromUser(savedUser);
    }

    /**
     * 删除用户
     */
    @Transactional
    public void deleteUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));
        
        // 检查用户是否有关联的设备
        List<Device> devices = deviceRepository.findByUserId(userId);
        if (!devices.isEmpty()) {
            throw new RuntimeException("用户有关联的设备，无法删除。请先解绑设备。");
        }
        
        userRepository.delete(user);
        log.info("用户删除成功: userId={}, username={}", userId, user.getUsername());
    }
}
