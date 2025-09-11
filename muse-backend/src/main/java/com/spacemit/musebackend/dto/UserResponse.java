package com.spacemit.musebackend.dto;

import com.spacemit.musebackend.entity.User;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserResponse {
    private Integer id;
    private String username;
    private String email;
    private String phone;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private Integer deviceCount;
    private List<DeviceInfo> devices;

    @Data
    public static class DeviceInfo {
        private String deviceId;
        private String deviceType;
        private String status;
    }

    public static UserResponse fromUser(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setStatus(user.getStatus().name());
        response.setCreatedAt(user.getCreatedAt());
        response.setLastLoginAt(user.getLastLoginAt());
        return response;
    }
}
