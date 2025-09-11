package com.spacemit.musebackend.service;

import com.spacemit.musebackend.dto.*;
import com.spacemit.musebackend.entity.Device;
import com.spacemit.musebackend.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public ApiResponse<HeartbeatResponse> processHeartbeat(HeartbeatRequest request) {
        try {
            Device device = deviceRepository.findByDeviceId(request.getDeviceId())
                    .orElseThrow(() -> new RuntimeException("设备不存在: " + request.getDeviceId()));

            // 更新设备状态
            device.setStatus(Device.DeviceStatus.ONLINE);
            device.setLastSeen(LocalDateTime.now());
            deviceRepository.save(device);

            HeartbeatResponse response = new HeartbeatResponse();
            response.setServerTime(LocalDateTime.now().toString());
            response.setStatus("online");
            response.setMessage("心跳接收成功");

            log.info("心跳接收成功: deviceId={}", request.getDeviceId());

            return ApiResponse.success("心跳接收成功", response);
        } catch (Exception e) {
            log.error("心跳处理失败: {}", e.getMessage(), e);
            return ApiResponse.error("心跳处理失败: " + e.getMessage());
        }
    }

    public ApiResponse<String> setDeviceOnline(String deviceId) {
        try {
            Device device = deviceRepository.findByDeviceId(deviceId)
                    .orElseThrow(() -> new RuntimeException("设备不存在: " + deviceId));

            // 更新设备状态为在线
            device.setStatus(Device.DeviceStatus.ONLINE);
            device.setLastSeen(LocalDateTime.now());
            deviceRepository.save(device);

            log.info("设备在线状态更新成功: deviceId={}", deviceId);
            return ApiResponse.success("设备上线通知成功", "设备已上线");
        } catch (Exception e) {
            log.error("设备在线状态更新失败: {}", e.getMessage(), e);
            return ApiResponse.error("设备在线状态更新失败: " + e.getMessage());
        }
    }

    public ApiResponse<String> setDeviceOffline(String deviceId) {
        try {
            Device device = deviceRepository.findByDeviceId(deviceId)
                    .orElseThrow(() -> new RuntimeException("设备不存在: " + deviceId));

            // 更新设备状态为离线
            device.setStatus(Device.DeviceStatus.OFFLINE);
            device.setLastSeen(LocalDateTime.now());
            deviceRepository.save(device);

            log.info("设备离线状态更新成功: deviceId={}", deviceId);
            return ApiResponse.success("设备离线通知成功", "设备已离线");
        } catch (Exception e) {
            log.error("设备离线状态更新失败: {}", e.getMessage(), e);
            return ApiResponse.error("设备离线状态更新失败: " + e.getMessage());
        }
    }

    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public void checkOfflineDevices() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(5); // 5分钟未收到心跳认为离线
            List<Device> devices = deviceRepository.findAll();

            for (Device device : devices) {
                if (device.getLastSeen() != null &&
                        device.getLastSeen().isBefore(threshold) &&
                        device.getStatus() == Device.DeviceStatus.ONLINE) {

                    device.setStatus(Device.DeviceStatus.LOST);
                    deviceRepository.save(device);

                    log.warn("设备离线: deviceId={}, lastSeen={}", device.getDeviceId(), device.getLastSeen());
                }
            }
        } catch (Exception e) {
            log.error("检查离线设备失败: {}", e.getMessage(), e);
        }
    }
}