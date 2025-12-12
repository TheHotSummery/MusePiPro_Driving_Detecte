package com.spacemit.musebackendv2.service.dashboard;

import com.spacemit.musebackendv2.dto.dashboard.*;
import com.spacemit.musebackendv2.entity.v2.*;
import com.spacemit.musebackendv2.repository.v2.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 实时监控数据服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardRealtimeService {

    private final DeviceV2Repository deviceRepository;
    private final DriverV2Repository driverRepository;
    private final DeviceDriverBindingV2Repository bindingRepository;
    private final StatusDataV2Repository statusDataRepository;
    private final GpsDataV2Repository gpsDataRepository;
    private final EventDataV2Repository eventDataRepository;

    /**
     * 获取实时车辆状态
     */
    public RealtimeVehiclesResponse getRealtimeVehicles() {
        List<DeviceV2> allDevices = deviceRepository.findAllActive();
        
        int totalVehicles = allDevices.size();
        int onlineVehicles = 0;
        int offlineVehicles = 0;
        
        List<VehicleStatusDTO> vehicles = new ArrayList<>();
        
        for (DeviceV2 device : allDevices) {
            // 判断在线/离线（最后上报时间在5分钟内视为在线）
            long now = System.currentTimeMillis();
            boolean isOnline = device.getLastReportTime() != null && 
                             (now - device.getLastReportTime()) < 5 * 60 * 1000;
            
            if (isOnline) {
                onlineVehicles++;
            } else {
                offlineVehicles++;
            }
            
            // 获取绑定的驾驶员
            String driverId = null;
            String driverName = null;
            Optional<DeviceDriverBindingV2> bindingOpt = bindingRepository.findActiveBindingByDeviceId(device.getDeviceId());
            if (bindingOpt.isPresent()) {
                driverId = bindingOpt.get().getDriverId();
                Optional<DriverV2> driverOpt = driverRepository.findByDriverId(driverId);
                if (driverOpt.isPresent()) {
                    driverName = driverOpt.get().getDriverName();
                }
            }
            
            // 获取最新状态
            String currentLevel = "Normal";
            Double currentScore = 0.0;
            Optional<StatusDataV2> latestStatusOpt = statusDataRepository.findLatestOneByDeviceId(device.getDeviceId());
            if (latestStatusOpt.isPresent()) {
                StatusDataV2 latestStatus = latestStatusOpt.get();
                currentLevel = latestStatus.getLevel();
                currentScore = latestStatus.getScore().doubleValue();
            }
            
            // 获取最新位置
            LocationDTO location = null;
            Optional<GpsDataV2> latestGpsOpt = gpsDataRepository.findLatestByDeviceId(device.getDeviceId());
            if (latestGpsOpt.isPresent()) {
                GpsDataV2 latestGps = latestGpsOpt.get();
                location = new LocationDTO();
                location.setLat(latestGps.getLocationLat());
                location.setLng(latestGps.getLocationLng());
                location.setSpeed(latestGps.getSpeed());
                location.setHeading(latestGps.getDirection());
                location.setAltitude(latestGps.getAltitude());
            }
            
            // 计算运行时长
            Long uptime = null;
            if (device.getFirstReportTime() != null) {
                uptime = (now - device.getFirstReportTime()) / 1000; // 秒
            }
            
            VehicleStatusDTO vehicle = new VehicleStatusDTO();
            vehicle.setDeviceId(device.getDeviceId());
            vehicle.setDriverId(driverId);
            vehicle.setDriverName(driverName);
            vehicle.setStatus(isOnline ? "online" : "offline");
            vehicle.setCurrentLevel(currentLevel);
            vehicle.setCurrentScore(currentScore);
            vehicle.setLocation(location);
            vehicle.setLastUpdateTime(device.getLastReportTime());
            vehicle.setUptime(uptime);
            
            vehicles.add(vehicle);
        }
        
        RealtimeVehiclesResponse response = new RealtimeVehiclesResponse();
        response.setTotalVehicles(totalVehicles);
        response.setOnlineVehicles(onlineVehicles);
        response.setOfflineVehicles(offlineVehicles);
        response.setVehicles(vehicles);
        
        return response;
    }

    /**
     * 获取实时告警事件
     * 返回最近1小时内的活跃告警（Level 1/2/3）
     */
    public RealtimeAlertsResponse getRealtimeAlerts() {
        long now = System.currentTimeMillis();
        long oneHourAgo = now - 60 * 60 * 1000; // 1小时前
        
        // 查询最近1小时内的告警事件（Level 1/2/3）
        List<EventDataV2> recentEvents = eventDataRepository.findByTimestampBetween(oneHourAgo, now)
            .stream()
            .filter(e -> !"Normal".equals(e.getLevel()))
            .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp())) // 按时间倒序
            .limit(100) // 最多返回100条
            .collect(Collectors.toList());
        
        int activeAlerts = recentEvents.size();
        int criticalAlerts = 0;
        int highAlerts = 0;
        int mediumAlerts = 0;
        
        List<AlertDTO> alerts = new ArrayList<>();
        
        for (EventDataV2 event : recentEvents) {
            // 统计各级别数量
            if ("Level 3".equals(event.getLevel())) {
                criticalAlerts++;
            } else if ("Level 2".equals(event.getLevel())) {
                highAlerts++;
            } else if ("Level 1".equals(event.getLevel())) {
                mediumAlerts++;
            }
            
            // 获取驾驶员信息
            String driverId = event.getDriverId();
            String driverName = null;
            if (driverId != null) {
                Optional<DriverV2> driverOpt = driverRepository.findByDriverId(driverId);
                if (driverOpt.isPresent()) {
                    driverName = driverOpt.get().getDriverName();
                }
            }
            
            // 构建位置信息
            LocationDTO location = null;
            if (event.getLocationLat() != null && event.getLocationLng() != null) {
                location = new LocationDTO();
                location.setLat(event.getLocationLat());
                location.setLng(event.getLocationLng());
                location.setAddress(event.getLocationAddress());
            }
            
            AlertDTO alert = new AlertDTO();
            alert.setAlertId(event.getEventId());
            alert.setDeviceId(event.getDeviceId());
            alert.setDriverId(driverId);
            alert.setDriverName(driverName);
            alert.setLevel(event.getLevel());
            alert.setScore(event.getScore());
            alert.setBehavior(event.getBehavior());
            alert.setLocation(location);
            alert.setTimestamp(event.getTimestamp());
            alert.setDuration(event.getDuration());
            alert.setStatus("active"); // 简化处理，所有都是活跃状态
            
            alerts.add(alert);
        }
        
        RealtimeAlertsResponse response = new RealtimeAlertsResponse();
        response.setActiveAlerts(activeAlerts);
        response.setCriticalAlerts(criticalAlerts);
        response.setHighAlerts(highAlerts);
        response.setMediumAlerts(mediumAlerts);
        response.setAlerts(alerts);
        
        return response;
    }

    /**
     * 获取系统运行状态
     */
    public SystemStatusResponse getSystemStatus() {
        List<DeviceV2> allDevices = deviceRepository.findAllActive();
        
        int totalDevices = allDevices.size();
        int healthyDevices = 0;
        int warningDevices = 0;
        int errorDevices = 0;
        
        double totalCpuUsage = 0.0;
        double totalMemoryUsage = 0.0;
        double totalTemperature = 0.0;
        int deviceCountWithStatus = 0;
        
        List<SystemStatusResponse.DeviceStatusDTO> deviceStatusList = new ArrayList<>();
        long now = System.currentTimeMillis();
        
        for (DeviceV2 device : allDevices) {
            // 获取最新状态数据
            Optional<StatusDataV2> latestStatusOpt = statusDataRepository.findLatestOneByDeviceId(device.getDeviceId());
            
            String status = "healthy";
            Double cpuUsage = null;
            Double memoryUsage = null;
            Double temperature = null;
            String networkStatus = "offline";
            Long lastHeartbeat = device.getLastReportTime();
            
            if (latestStatusOpt.isPresent()) {
                StatusDataV2 statusData = latestStatusOpt.get();
                cpuUsage = statusData.getCpuUsage() != null ? statusData.getCpuUsage().doubleValue() : null;
                memoryUsage = statusData.getMemoryUsage() != null ? statusData.getMemoryUsage().doubleValue() : null;
                temperature = statusData.getTemperature() != null ? statusData.getTemperature().doubleValue() : null;
                
                // 判断网络状态（最后上报时间在5分钟内视为在线）
                if (lastHeartbeat != null && (now - lastHeartbeat) < 5 * 60 * 1000) {
                    networkStatus = "online";
                }
                
                // 判断设备健康状态
                if (networkStatus.equals("offline")) {
                    status = "error";
                    errorDevices++;
                } else if (cpuUsage != null && cpuUsage > 80) {
                    status = "warning";
                    warningDevices++;
                } else if (memoryUsage != null && memoryUsage > 85) {
                    status = "warning";
                    warningDevices++;
                } else if (temperature != null && temperature > 70) {
                    status = "warning";
                    warningDevices++;
                } else {
                    status = "healthy";
                    healthyDevices++;
                }
                
                // 累计统计数据
                if (cpuUsage != null) {
                    totalCpuUsage += cpuUsage;
                    deviceCountWithStatus++;
                }
                if (memoryUsage != null) {
                    totalMemoryUsage += memoryUsage;
                }
                if (temperature != null) {
                    totalTemperature += temperature;
                }
            } else {
                // 没有状态数据，判断为离线
                status = "error";
                errorDevices++;
            }
            
            SystemStatusResponse.DeviceStatusDTO deviceStatus = new SystemStatusResponse.DeviceStatusDTO();
            deviceStatus.setDeviceId(device.getDeviceId());
            deviceStatus.setStatus(status);
            deviceStatus.setCpuUsage(cpuUsage);
            deviceStatus.setMemoryUsage(memoryUsage);
            deviceStatus.setTemperature(temperature);
            deviceStatus.setNetworkStatus(networkStatus);
            deviceStatus.setLastHeartbeat(lastHeartbeat);
            
            deviceStatusList.add(deviceStatus);
        }
        
        // 计算平均值
        SystemStatusResponse.SystemStatsDTO systemStats = new SystemStatusResponse.SystemStatsDTO();
        if (deviceCountWithStatus > 0) {
            systemStats.setAvgCpuUsage(totalCpuUsage / deviceCountWithStatus);
            systemStats.setAvgMemoryUsage(totalMemoryUsage / deviceCountWithStatus);
            systemStats.setAvgTemperature(totalTemperature / deviceCountWithStatus);
        } else {
            systemStats.setAvgCpuUsage(0.0);
            systemStats.setAvgMemoryUsage(0.0);
            systemStats.setAvgTemperature(0.0);
        }
        systemStats.setAvgNetworkLatency(120); // 简化处理，固定值
        
        SystemStatusResponse response = new SystemStatusResponse();
        response.setTotalDevices(totalDevices);
        response.setHealthyDevices(healthyDevices);
        response.setWarningDevices(warningDevices);
        response.setErrorDevices(errorDevices);
        response.setSystemStats(systemStats);
        response.setDeviceStatus(deviceStatusList);
        
        return response;
    }
}

