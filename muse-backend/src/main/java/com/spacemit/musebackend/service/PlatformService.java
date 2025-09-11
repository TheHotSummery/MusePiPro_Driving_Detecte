package com.spacemit.musebackend.service;

import com.spacemit.musebackend.entity.Device;
import com.spacemit.musebackend.entity.Event;
import com.spacemit.musebackend.entity.RealtimeData;
import com.spacemit.musebackend.entity.User;
import com.spacemit.musebackend.repository.DeviceRepository;
import com.spacemit.musebackend.repository.EventRepository;
import com.spacemit.musebackend.repository.RealtimeDataRepository;
import com.spacemit.musebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final RealtimeDataRepository realtimeDataRepository;
    private final EventRepository eventRepository;

    /**
     * 获取系统概览
     */
    public Map<String, Object> getSystemOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        // 设备状态统计
        List<Device> allDevices = deviceRepository.findAll();
        Map<String, Object> deviceStatus = new HashMap<>();
        deviceStatus.put("total", allDevices.size());
        deviceStatus.put("online", allDevices.stream().mapToInt(d -> Device.DeviceStatus.ONLINE.equals(d.getStatus()) ? 1 : 0).sum());
        deviceStatus.put("offline", allDevices.stream().mapToInt(d -> Device.DeviceStatus.OFFLINE.equals(d.getStatus()) ? 1 : 0).sum());
        deviceStatus.put("lost", allDevices.stream().mapToInt(d -> Device.DeviceStatus.LOST.equals(d.getStatus()) ? 1 : 0).sum());
        overview.put("deviceStatus", deviceStatus);
        
        // 今日事件统计
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        List<Event> todayEvents = eventRepository.findByTimestampBetween(todayStart, todayEnd);
        
        Map<String, Object> todayEventSummary = new HashMap<>();
        todayEventSummary.put("total", todayEvents.size());
        todayEventSummary.put("fatigue", todayEvents.stream().mapToInt(e -> Event.EventType.FATIGUE.equals(e.getEventType()) ? 1 : 0).sum());
        todayEventSummary.put("distraction", todayEvents.stream().mapToInt(e -> Event.EventType.DISTRACTION.equals(e.getEventType()) ? 1 : 0).sum());
        todayEventSummary.put("emergency", todayEvents.stream().mapToInt(e -> Event.EventType.EMERGENCY.equals(e.getEventType()) ? 1 : 0).sum());
        Map<String, Object> eventSummary = new HashMap<>();
        eventSummary.put("today", todayEventSummary);
        overview.put("eventSummary", eventSummary);
        
        // 告警统计
        Map<String, Object> alertSummary = new HashMap<>();
        alertSummary.put("active", todayEvents.stream().mapToInt(e -> Event.Severity.HIGH.equals(e.getSeverity()) || Event.Severity.CRITICAL.equals(e.getSeverity()) ? 1 : 0).sum());
        alertSummary.put("critical", todayEvents.stream().mapToInt(e -> Event.Severity.CRITICAL.equals(e.getSeverity()) ? 1 : 0).sum());
        alertSummary.put("acknowledged", 0); // 暂时设为0，后续可以添加确认字段
        overview.put("alertSummary", alertSummary);
        
        // 性能指标
        Map<String, Object> performanceMetrics = new HashMap<>();
        performanceMetrics.put("averageResponseTime", 150);
        performanceMetrics.put("systemUptime", 99.9);
        performanceMetrics.put("dataAccuracy", 98.5);
        overview.put("performanceMetrics", performanceMetrics);
        
        return overview;
    }

    /**
     * 获取设备列表
     */
    public Map<String, Object> getDevices(String status, String deviceType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Device> devicePage;
        
        if (status != null && deviceType != null) {
            Device.DeviceStatus deviceStatus = Device.DeviceStatus.valueOf(status);
            devicePage = deviceRepository.findByStatusAndDeviceType(deviceStatus, deviceType, pageable);
        } else if (status != null) {
            Device.DeviceStatus deviceStatus = Device.DeviceStatus.valueOf(status);
            devicePage = deviceRepository.findByStatus(deviceStatus, pageable);
        } else if (deviceType != null) {
            devicePage = deviceRepository.findByDeviceType(deviceType, pageable);
        } else {
            devicePage = deviceRepository.findAll(pageable);
        }
        
        List<Map<String, Object>> devices = devicePage.getContent().stream().map(device -> {
            Map<String, Object> deviceInfo = new HashMap<>();
            deviceInfo.put("deviceId", device.getDeviceId());
            deviceInfo.put("deviceType", device.getDeviceType());
            deviceInfo.put("version", device.getVersion());
            deviceInfo.put("userId", device.getUserId());
            deviceInfo.put("status", device.getStatus());
            deviceInfo.put("lastSeen", device.getLastSeen());
            
            // 获取用户信息
            if (device.getUserId() != null) {
                Optional<User> userOpt = userRepository.findById(device.getUserId());
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    deviceInfo.put("username", user.getUsername());
                }
            }
            
            // 获取最新位置
            Optional<RealtimeData> latestData = realtimeDataRepository.findTopByDeviceIdOrderByTimestampDesc(device.getDeviceId());
            if (latestData.isPresent()) {
                RealtimeData data = latestData.get();
                Map<String, Object> location = new HashMap<>();
                location.put("lat", data.getLocationLat());
                location.put("lng", data.getLocationLng());
                deviceInfo.put("location", location);
            }
            
            // 计算健康评分
            deviceInfo.put("healthScore", calculateHealthScore(device.getDeviceId()));
            
            // 计算总驾驶时间和距离
            Map<String, Object> statistics = calculateDeviceStatistics(device.getDeviceId());
            deviceInfo.put("totalDrivingTime", statistics.get("totalDrivingTime"));
            deviceInfo.put("totalDistance", statistics.get("totalDistance"));
            
            return deviceInfo;
        }).collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("devices", devices);
        result.put("total", devicePage.getTotalElements());
        result.put("onlineCount", devices.stream().mapToInt(d -> Device.DeviceStatus.ONLINE.toString().equals(d.get("status")) ? 1 : 0).sum());
        result.put("offlineCount", devices.stream().mapToInt(d -> Device.DeviceStatus.OFFLINE.toString().equals(d.get("status")) ? 1 : 0).sum());
        
        return result;
    }

    /**
     * 获取设备详情
     */
    public Map<String, Object> getDeviceDetail(String deviceId) {
        Optional<Device> deviceOpt = deviceRepository.findByDeviceId(deviceId);
        if (!deviceOpt.isPresent()) {
            throw new RuntimeException("设备不存在: " + deviceId);
        }
        
        Device device = deviceOpt.get();
        Map<String, Object> deviceDetail = new HashMap<>();
        deviceDetail.put("deviceId", device.getDeviceId());
        deviceDetail.put("deviceType", device.getDeviceType());
        deviceDetail.put("version", device.getVersion());
        deviceDetail.put("status", device.getStatus());
        deviceDetail.put("lastSeen", device.getLastSeen());
        
        // 获取用户信息
        if (device.getUserId() != null) {
            Optional<User> userOpt = userRepository.findById(device.getUserId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("username", user.getUsername());
                userInfo.put("email", user.getEmail());
                userInfo.put("phone", user.getPhone());
                deviceDetail.put("user", userInfo);
            }
        }
        
        // 获取当前位置
        Optional<RealtimeData> latestData = realtimeDataRepository.findTopByDeviceIdOrderByTimestampDesc(deviceId);
        if (latestData.isPresent()) {
            RealtimeData data = latestData.get();
            Map<String, Object> currentLocation = new HashMap<>();
            currentLocation.put("lat", data.getLocationLat());
            currentLocation.put("lng", data.getLocationLng());
            currentLocation.put("speed", data.getSpeed());
            currentLocation.put("direction", data.getDirection());
            currentLocation.put("altitude", data.getAltitude());
            currentLocation.put("hdop", data.getHdop());
            currentLocation.put("satellites", data.getSatellites());
            deviceDetail.put("currentLocation", currentLocation);
        }
        
        // 获取统计信息
        Map<String, Object> statistics = calculateDeviceStatistics(deviceId);
        deviceDetail.put("statistics", statistics);
        
        // 计算健康评分
        deviceDetail.put("healthScore", calculateHealthScore(deviceId));
        
        return deviceDetail;
    }

    /**
     * 获取设备轨迹
     */
    public Map<String, Object> getDeviceTrack(String deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        List<RealtimeData> trackData = realtimeDataRepository.findByDeviceIdAndTimestampBetweenOrderByTimestampAsc(
                deviceId, startTime, endTime);
        
        List<Map<String, Object>> track = trackData.stream().map(data -> {
            Map<String, Object> point = new HashMap<>();
            point.put("timestamp", data.getTimestamp());
            point.put("lat", data.getLocationLat());
            point.put("lng", data.getLocationLng());
            point.put("speed", data.getSpeed());
            point.put("direction", data.getDirection());
            
            // 检查该时间点是否有事件
            List<Event> events = eventRepository.findByDeviceIdAndTimestampBetween(
                    deviceId, data.getTimestamp().minusSeconds(30), data.getTimestamp().plusSeconds(30));
            if (!events.isEmpty()) {
                Event event = events.get(0);
                point.put("eventType", event.getEventType());
                point.put("severity", event.getSeverity());
            } else {
                point.put("eventType", null);
                point.put("severity", null);
            }
            
            return point;
        }).collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("deviceId", deviceId);
        result.put("track", track);
        result.put("totalPoints", track.size());
        result.put("totalDistance", calculateTotalDistance(track));
        result.put("totalTime", trackData.isEmpty() ? 0 : 
                java.time.Duration.between(trackData.get(0).getTimestamp(), 
                        trackData.get(trackData.size() - 1).getTimestamp()).getSeconds());
        
        return result;
    }

    /**
     * 获取实时数据流
     */
    public Map<String, Object> getRealtimeStream() {
        List<Device> onlineDevices = deviceRepository.findByStatus(Device.DeviceStatus.ONLINE);
        
        List<Map<String, Object>> devices = onlineDevices.stream().map(device -> {
            Map<String, Object> deviceInfo = new HashMap<>();
            deviceInfo.put("deviceId", device.getDeviceId());
            deviceInfo.put("timestamp", device.getLastSeen());
            deviceInfo.put("status", device.getStatus());
            
            // 获取最新位置
            Optional<RealtimeData> latestData = realtimeDataRepository.findTopByDeviceIdOrderByTimestampDesc(device.getDeviceId());
            if (latestData.isPresent()) {
                RealtimeData data = latestData.get();
                Map<String, Object> location = new HashMap<>();
                location.put("lat", data.getLocationLat());
                location.put("lng", data.getLocationLng());
                deviceInfo.put("location", location);
                deviceInfo.put("speed", data.getSpeed());
                deviceInfo.put("direction", data.getDirection());
            }
            
            // 获取疲劳等级
            deviceInfo.put("fatigueLevel", getCurrentFatigueLevel(device.getDeviceId()));
            deviceInfo.put("alertLevel", getCurrentAlertLevel(device.getDeviceId()));
            
            return deviceInfo;
        }).collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("devices", devices);
        return result;
    }

    /**
     * 获取事件列表
     */
    public Map<String, Object> getEvents(String deviceId, String eventType, String severity, 
                                        LocalDateTime startTime, LocalDateTime endTime, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Event> eventPage;
        
        // 如果所有参数都为空，返回所有数据（不限制时间范围）
        if (deviceId == null && eventType == null && severity == null && startTime == null && endTime == null) {
            eventPage = eventRepository.findAll(pageable);
        } else {
            // 只有当明确指定了时间参数时才设置时间范围，否则不限制时间
            boolean hasTimeFilter = startTime != null || endTime != null;
            if (!hasTimeFilter) {
                // 没有时间参数时，不限制时间范围
                startTime = null;
                endTime = null;
            } else {
                // 设置默认时间范围（如果只指定了部分时间参数）
                if (startTime == null) {
                    startTime = LocalDateTime.now().minusDays(7);
                }
                if (endTime == null) {
                    endTime = LocalDateTime.now();
                }
            }
            
            // 根据参数组合选择查询方法
            if (deviceId != null && eventType != null && severity != null) {
                // 三个条件都有
                Event.EventType eventTypeEnum = Event.EventType.valueOf(eventType);
                Event.Severity severityEnum = Event.Severity.valueOf(severity);
                if (hasTimeFilter) {
                    eventPage = eventRepository.findByDeviceIdAndEventTypeAndSeverityAndTimestampBetween(
                            deviceId, eventTypeEnum, severityEnum, startTime, endTime, pageable);
                } else {
                    // 需要添加无时间限制的查询方法
                    eventPage = eventRepository.findByDeviceIdAndEventTypeAndSeverity(deviceId, eventTypeEnum, severityEnum, pageable);
                }
            } else if (deviceId != null && eventType != null) {
                // 设备ID + 事件类型
                Event.EventType eventTypeEnum = Event.EventType.valueOf(eventType);
                if (hasTimeFilter) {
                    eventPage = eventRepository.findByDeviceIdAndEventTypeAndTimestampBetween(
                            deviceId, eventTypeEnum, startTime, endTime, pageable);
                } else {
                    eventPage = eventRepository.findByDeviceIdAndEventType(deviceId, eventTypeEnum, pageable);
                }
            } else if (deviceId != null && severity != null) {
                // 设备ID + 严重程度
                Event.Severity severityEnum = Event.Severity.valueOf(severity);
                if (hasTimeFilter) {
                    eventPage = eventRepository.findByDeviceIdAndSeverityAndTimestampBetween(
                            deviceId, severityEnum, startTime, endTime, pageable);
                } else {
                    eventPage = eventRepository.findByDeviceIdAndSeverity(deviceId, severityEnum, pageable);
                }
            } else if (eventType != null && severity != null) {
                // 事件类型 + 严重程度
                Event.EventType eventTypeEnum = Event.EventType.valueOf(eventType);
                Event.Severity severityEnum = Event.Severity.valueOf(severity);
                if (hasTimeFilter) {
                    eventPage = eventRepository.findByEventTypeAndSeverityAndTimestampBetween(
                            eventTypeEnum, severityEnum, startTime, endTime, pageable);
                } else {
                    eventPage = eventRepository.findByEventTypeAndSeverity(eventTypeEnum, severityEnum, pageable);
                }
            } else if (deviceId != null) {
                // 只有设备ID
                if (hasTimeFilter) {
                    eventPage = eventRepository.findByDeviceIdAndTimestampBetween(deviceId, startTime, endTime, pageable);
                } else {
                    eventPage = eventRepository.findByDeviceId(deviceId, pageable);
                }
            } else if (eventType != null) {
                // 只有事件类型
                Event.EventType eventTypeEnum = Event.EventType.valueOf(eventType);
                if (hasTimeFilter) {
                    eventPage = eventRepository.findByEventTypeAndTimestampBetween(
                            eventTypeEnum, startTime, endTime, pageable);
                } else {
                    eventPage = eventRepository.findByEventType(eventTypeEnum, pageable);
                }
            } else if (severity != null) {
                // 只有严重程度
                Event.Severity severityEnum = Event.Severity.valueOf(severity);
                if (hasTimeFilter) {
                    eventPage = eventRepository.findBySeverityAndTimestampBetween(
                            severityEnum, startTime, endTime, pageable);
                } else {
                    eventPage = eventRepository.findBySeverity(severityEnum, pageable);
                }
            } else {
                // 只有时间范围
                eventPage = eventRepository.findByTimestampBetween(startTime, endTime, pageable);
            }
        }
        
        List<Map<String, Object>> events = eventPage.getContent().stream().map(event -> {
            Map<String, Object> eventInfo = new HashMap<>();
            eventInfo.put("eventId", event.getEventId());
            eventInfo.put("deviceId", event.getDeviceId());
            eventInfo.put("timestamp", event.getTimestamp());
            eventInfo.put("eventType", event.getEventType());
            eventInfo.put("severity", event.getSeverity());
            eventInfo.put("behavior", event.getBehavior());
            eventInfo.put("confidence", event.getConfidence());
            eventInfo.put("duration", event.getDuration());
            eventInfo.put("alertLevel", event.getAlertLevel());
            eventInfo.put("context", event.getContext());
            
            // 位置信息
            if (event.getLocationLat() != null && event.getLocationLng() != null) {
                Map<String, Object> location = new HashMap<>();
                location.put("lat", event.getLocationLat());
                location.put("lng", event.getLocationLng());
                eventInfo.put("location", location);
            }
            
            // 获取用户信息
            Optional<Device> deviceOpt = deviceRepository.findByDeviceId(event.getDeviceId());
            if (deviceOpt.isPresent() && deviceOpt.get().getUserId() != null) {
                Optional<User> userOpt = userRepository.findById(deviceOpt.get().getUserId());
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("username", user.getUsername());
                    userInfo.put("phone", user.getPhone());
                    eventInfo.put("user", userInfo);
                }
            }
            
            return eventInfo;
        }).collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("events", events);
        result.put("total", eventPage.getTotalElements());
        
        // 统计信息
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("fatigueCount", events.stream().mapToInt(e -> Event.EventType.FATIGUE.equals(e.get("eventType")) ? 1 : 0).sum());
        statistics.put("distractionCount", events.stream().mapToInt(e -> Event.EventType.DISTRACTION.equals(e.get("eventType")) ? 1 : 0).sum());
        statistics.put("emergencyCount", events.stream().mapToInt(e -> Event.EventType.EMERGENCY.equals(e.get("eventType")) ? 1 : 0).sum());
        statistics.put("highSeverityCount", events.stream().mapToInt(e -> Event.Severity.HIGH.equals(e.get("severity")) ? 1 : 0).sum());
        result.put("statistics", statistics);
        
        return result;
    }

    /**
     * 获取事件统计
     */
    public Map<String, Object> getEventStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null) {
            startTime = LocalDateTime.now().minusDays(7);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }
        
        List<Event> events = eventRepository.findByTimestampBetween(startTime, endTime);
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalEvents", events.size());
        
        // 事件类型统计
        Map<String, Long> eventTypes = events.stream()
                .collect(Collectors.groupingBy(e -> e.getEventType().toString(), Collectors.counting()));
        result.put("eventTypes", eventTypes);
        
        // 严重程度统计
        Map<String, Long> severityLevels = events.stream()
                .collect(Collectors.groupingBy(e -> e.getSeverity().toString(), Collectors.counting()));
        result.put("severityLevels", severityLevels);
        
        // 小时分布统计
        Map<Integer, Long> hourlyDistribution = events.stream()
                .collect(Collectors.groupingBy(e -> e.getTimestamp().getHour(), Collectors.counting()));
        List<Map<String, Object>> hourlyList = hourlyDistribution.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> hourData = new HashMap<>();
                    hourData.put("hour", entry.getKey());
                    hourData.put("count", entry.getValue());
                    return hourData;
                })
                .sorted(Comparator.comparing(h -> (Integer) h.get("hour")))
                .collect(Collectors.toList());
        result.put("hourlyDistribution", hourlyList);
        
        // 设备统计
        Map<String, Map<String, Object>> deviceStatistics = events.stream()
                .collect(Collectors.groupingBy(Event::getDeviceId))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            List<Event> deviceEvents = entry.getValue();
                            Map<String, Object> deviceStats = new HashMap<>();
                            deviceStats.put("eventCount", deviceEvents.size());
                            deviceStats.put("fatigueCount", deviceEvents.stream().mapToInt(e -> Event.EventType.FATIGUE.equals(e.getEventType()) ? 1 : 0).sum());
                            deviceStats.put("distractionCount", deviceEvents.stream().mapToInt(e -> Event.EventType.DISTRACTION.equals(e.getEventType()) ? 1 : 0).sum());
                            
                            // 获取用户名
                            Optional<Device> deviceOpt = deviceRepository.findByDeviceId(entry.getKey());
                            if (deviceOpt.isPresent() && deviceOpt.get().getUserId() != null) {
                                Optional<User> userOpt = userRepository.findById(deviceOpt.get().getUserId());
                                if (userOpt.isPresent()) {
                                    deviceStats.put("username", userOpt.get().getUsername());
                                }
                            }
                            
                            return deviceStats;
                        }
                ));
        result.put("deviceStatistics", deviceStatistics);
        
        return result;
    }

    /**
     * 获取实时告警
     */
    public Map<String, Object> getRealtimeAlerts() {
        // 为了测试方便，暂时查询所有HIGH和CRITICAL级别的事件
        // 生产环境应该使用: LocalDateTime.now().minusHours(1)
        List<Event> recentEvents = eventRepository.findBySeverityIn(
                Arrays.asList(Event.Severity.HIGH, Event.Severity.CRITICAL));
        
        List<Map<String, Object>> alerts = recentEvents.stream().map(event -> {
            Map<String, Object> alert = new HashMap<>();
            alert.put("alertId", "ALERT_" + event.getEventId());
            alert.put("deviceId", event.getDeviceId());
            alert.put("alertType", event.getEventType() + "_" + event.getSeverity());
            alert.put("severity", event.getSeverity());
            alert.put("timestamp", event.getTimestamp());
            alert.put("message", generateAlertMessage(event));
            alert.put("status", "ACTIVE");
            alert.put("acknowledged", false);
            
            // 位置信息
            if (event.getLocationLat() != null && event.getLocationLng() != null) {
                Map<String, Object> location = new HashMap<>();
                location.put("lat", event.getLocationLat());
                location.put("lng", event.getLocationLng());
                alert.put("location", location);
            }
            
            // 用户信息
            Optional<Device> deviceOpt = deviceRepository.findByDeviceId(event.getDeviceId());
            if (deviceOpt.isPresent() && deviceOpt.get().getUserId() != null) {
                Optional<User> userOpt = userRepository.findById(deviceOpt.get().getUserId());
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("username", user.getUsername());
                    userInfo.put("phone", user.getPhone());
                    alert.put("user", userInfo);
                }
            }
            
            return alert;
        }).collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("alerts", alerts);
        result.put("activeCount", alerts.size());
        result.put("criticalCount", alerts.stream().mapToInt(a -> "CRITICAL".equals(a.get("severity")) ? 1 : 0).sum());
        
        return result;
    }

    /**
     * 获取告警历史
     */
    public Map<String, Object> getAlertHistory(LocalDateTime startTime, LocalDateTime endTime, int page, int size) {
        // 这里可以扩展为专门的告警表，暂时使用事件表
        return getEvents(null, null, null, startTime, endTime, page, size);
    }

    /**
     * 获取驾驶行为分析
     */
    public Map<String, Object> getDrivingBehaviorAnalysis(String deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null) {
            startTime = LocalDateTime.now().minusDays(1);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("deviceId", deviceId);
        result.put("period", Map.of("startTime", startTime, "endTime", endTime));
        
        // 获取设备信息
        Optional<Device> deviceOpt = deviceRepository.findByDeviceId(deviceId);
        if (deviceOpt.isPresent() && deviceOpt.get().getUserId() != null) {
            Optional<User> userOpt = userRepository.findById(deviceOpt.get().getUserId());
            if (userOpt.isPresent()) {
                result.put("username", userOpt.get().getUsername());
            }
        }
        
        // 获取实时数据
        List<RealtimeData> realtimeData = realtimeDataRepository.findByDeviceIdAndTimestampBetweenOrderByTimestampAsc(
                deviceId, startTime, endTime);
        
        // 获取事件数据
        List<Event> events = eventRepository.findByDeviceIdAndTimestampBetween(deviceId, startTime, endTime);
        
        // 计算统计信息
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalDrivingTime", realtimeData.isEmpty() ? 0 : 
                java.time.Duration.between(realtimeData.get(0).getTimestamp(), 
                        realtimeData.get(realtimeData.size() - 1).getTimestamp()).getSeconds());
        statistics.put("totalDistance", calculateTotalDistanceFromRealtimeData(realtimeData));
        statistics.put("averageSpeed", realtimeData.stream()
                .filter(d -> d.getSpeed() != null)
                .mapToDouble(d -> d.getSpeed().doubleValue())
                .average().orElse(0.0));
        statistics.put("maxSpeed", realtimeData.stream()
                .filter(d -> d.getSpeed() != null)
                .mapToDouble(d -> d.getSpeed().doubleValue())
                .max().orElse(0.0));
        statistics.put("fatigueEvents", events.stream().mapToInt(e -> Event.EventType.FATIGUE.equals(e.getEventType()) ? 1 : 0).sum());
        statistics.put("distractionEvents", events.stream().mapToInt(e -> Event.EventType.DISTRACTION.equals(e.getEventType()) ? 1 : 0).sum());
        statistics.put("fatigueRiskScore", calculateFatigueRiskScore(events));
        statistics.put("safetyScore", calculateSafetyScore(events, realtimeData));
        result.put("statistics", statistics);
        
        // 小时分析
        Map<Integer, List<RealtimeData>> hourlyData = realtimeData.stream()
                .collect(Collectors.groupingBy(data -> data.getTimestamp().getHour()));
        
        List<Map<String, Object>> hourlyAnalysis = hourlyData.entrySet().stream()
                .map(entry -> {
                    List<RealtimeData> hourData = entry.getValue();
                    Map<String, Object> analysis = new HashMap<>();
                    analysis.put("hour", entry.getKey());
                    analysis.put("drivingTime", hourData.size() * 20); // 假设每20秒一条数据
                    analysis.put("distance", calculateTotalDistanceFromRealtimeData(hourData));
                    analysis.put("fatigueEvents", events.stream().mapToInt(e -> 
                            e.getTimestamp().getHour() == entry.getKey() && Event.EventType.FATIGUE.equals(e.getEventType()) ? 1 : 0).sum());
                    analysis.put("averageSpeed", hourData.stream()
                            .filter(d -> d.getSpeed() != null)
                            .mapToDouble(d -> d.getSpeed().doubleValue())
                            .average().orElse(0.0));
                    return analysis;
                })
                .sorted(Comparator.comparing(h -> (Integer) h.get("hour")))
                .collect(Collectors.toList());
        result.put("hourlyAnalysis", hourlyAnalysis);
        
        return result;
    }

    // 辅助方法
    private int calculateHealthScore(String deviceId) {
        // 基于心跳间隔、GPS质量等计算健康评分
        Optional<Device> deviceOpt = deviceRepository.findByDeviceId(deviceId);
        if (!deviceOpt.isPresent()) {
            return 0;
        }
        
        Device device = deviceOpt.get();
        int score = 100;
        
        // 检查在线状态
        if (!Device.DeviceStatus.ONLINE.equals(device.getStatus())) {
            score -= 50;
        }
        
        // 检查最后心跳时间
        if (device.getLastSeen() != null) {
            long minutesSinceLastSeen = java.time.Duration.between(device.getLastSeen(), LocalDateTime.now()).toMinutes();
            if (minutesSinceLastSeen > 5) {
                score -= Math.min(30, minutesSinceLastSeen - 5);
            }
        }

        // 检查GPS质量
        Optional<RealtimeData> latestData = realtimeDataRepository.findTopByDeviceIdOrderByTimestampDesc(deviceId);
        if (latestData.isPresent()) {
            RealtimeData data = latestData.get();
            if (data.getHdop() != null && data.getHdop().doubleValue() > 5) {
                score -= 20;
            }
            if (data.getSatellites() != null && data.getSatellites() < 4) {
                score -= 15;
            }
        }
        
        return Math.max(0, score);
    }

    private Map<String, Object> calculateDeviceStatistics(String deviceId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 计算总驾驶时间（基于实时数据）
        List<RealtimeData> allData = realtimeDataRepository.findByDeviceIdOrderByTimestampAsc(deviceId);
        if (!allData.isEmpty()) {
            long totalSeconds = java.time.Duration.between(
                    allData.get(0).getTimestamp(), 
                    allData.get(allData.size() - 1).getTimestamp()).getSeconds();
            stats.put("totalDrivingTime", totalSeconds);
            stats.put("totalDistance", calculateTotalDistanceFromRealtimeData(allData));
        } else {
            stats.put("totalDrivingTime", 0);
            stats.put("totalDistance", 0.0);
        }
        
        // 计算事件统计
        List<Event> events = eventRepository.findByDeviceId(deviceId);
        stats.put("fatigueEvents", events.stream().mapToInt(e -> Event.EventType.FATIGUE.equals(e.getEventType()) ? 1 : 0).sum());
        stats.put("distractionEvents", events.stream().mapToInt(e -> Event.EventType.DISTRACTION.equals(e.getEventType()) ? 1 : 0).sum());
        
        // 最后疲劳事件时间
        Optional<Event> lastFatigueEvent = events.stream()
                .filter(e -> Event.EventType.FATIGUE.equals(e.getEventType()))
                .max(Comparator.comparing(Event::getTimestamp));
        stats.put("lastFatigueEvent", lastFatigueEvent.map(Event::getTimestamp).orElse(null));
        
        return stats;
    }

    private double calculateTotalDistance(List<Map<String, Object>> track) {
        if (track.size() < 2) return 0.0;
        
        double totalDistance = 0.0;
        for (int i = 1; i < track.size(); i++) {
            Map<String, Object> prev = track.get(i - 1);
            Map<String, Object> curr = track.get(i);
            
            double lat1 = (Double) prev.get("lat");
            double lng1 = (Double) prev.get("lng");
            double lat2 = (Double) curr.get("lat");
            double lng2 = (Double) curr.get("lng");
            
            totalDistance += calculateDistance(lat1, lng1, lat2, lng2);
        }
        
        return totalDistance;
    }

    private double calculateTotalDistanceFromRealtimeData(List<RealtimeData> data) {
        if (data.size() < 2) return 0.0;
        
        double totalDistance = 0.0;
        for (int i = 1; i < data.size(); i++) {
            RealtimeData prev = data.get(i - 1);
            RealtimeData curr = data.get(i);
            
            if (prev.getLocationLat() != null && prev.getLocationLng() != null &&
                curr.getLocationLat() != null && curr.getLocationLng() != null) {
                totalDistance += calculateDistance(prev.getLocationLat(), prev.getLocationLng(),
                        curr.getLocationLat(), curr.getLocationLng());
            }
        }
        
        return totalDistance;
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        // 简化的距离计算（实际应该使用更精确的公式）
        double R = 6371; // 地球半径（公里）
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
    
    private double calculateDistance(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        // BigDecimal版本的距离计算
        return calculateDistance(lat1.doubleValue(), lng1.doubleValue(), lat2.doubleValue(), lng2.doubleValue());
    }

    private String getCurrentFatigueLevel(String deviceId) {
        LocalDateTime recentTime = LocalDateTime.now().minusMinutes(10);
        List<Event> recentFatigueEvents = eventRepository.findByDeviceIdAndEventTypeAndTimestampAfter(
                deviceId, Event.EventType.FATIGUE, recentTime);
        
        if (recentFatigueEvents.isEmpty()) {
            return "LOW";
        }
        
        long highSeverityCount = recentFatigueEvents.stream()
                .mapToInt(e -> "HIGH".equals(e.getSeverity()) || "CRITICAL".equals(e.getSeverity()) ? 1 : 0).sum();
        
        if (highSeverityCount > 0) {
            return "HIGH";
        } else if (recentFatigueEvents.size() > 2) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private String getCurrentAlertLevel(String deviceId) {
        LocalDateTime recentTime = LocalDateTime.now().minusMinutes(5);
        List<Event> recentEvents = eventRepository.findByDeviceIdAndTimestampAfter(deviceId, recentTime);
        
        if (recentEvents.isEmpty()) {
            return "NORMAL";
        }
        
        boolean hasCritical = recentEvents.stream().anyMatch(e -> "CRITICAL".equals(e.getSeverity()));
        boolean hasHigh = recentEvents.stream().anyMatch(e -> "HIGH".equals(e.getSeverity()));
        
        if (hasCritical) {
            return "CRITICAL";
        } else if (hasHigh) {
            return "HIGH";
        } else {
            return "MEDIUM";
        }
    }

    private String generateAlertMessage(Event event) {
        String username = "未知用户";
        Optional<Device> deviceOpt = deviceRepository.findByDeviceId(event.getDeviceId());
        if (deviceOpt.isPresent() && deviceOpt.get().getUserId() != null) {
            Optional<User> userOpt = userRepository.findById(deviceOpt.get().getUserId());
            if (userOpt.isPresent()) {
                username = userOpt.get().getUsername();
            }
        }
        
        String eventTypeName = Event.EventType.FATIGUE.equals(event.getEventType()) ? "疲劳" : 
                              Event.EventType.DISTRACTION.equals(event.getEventType()) ? "分心" : "紧急";
        String severityName = Event.Severity.HIGH.equals(event.getSeverity()) ? "严重" : 
                             Event.Severity.CRITICAL.equals(event.getSeverity()) ? "危急" : "中等";
        
        return String.format("驾驶员%s出现%s%s状态", username, severityName, eventTypeName);
    }

    private int calculateFatigueRiskScore(List<Event> events) {
        if (events.isEmpty()) return 0;
        
        long fatigueEvents = events.stream().mapToInt(e -> Event.EventType.FATIGUE.equals(e.getEventType()) ? 1 : 0).sum();
        long highSeverityEvents = events.stream().mapToInt(e -> 
                Event.Severity.HIGH.equals(e.getSeverity()) || Event.Severity.CRITICAL.equals(e.getSeverity()) ? 1 : 0).sum();
        
        int score = (int) (fatigueEvents * 10 + highSeverityEvents * 20);
        return Math.min(100, score);
    }

    private int calculateSafetyScore(List<Event> events, List<RealtimeData> realtimeData) {
        int score = 100;
        
        // 基于事件数量扣分
        score -= events.size() * 5;
        
        // 基于超速扣分
        long speedingCount = realtimeData.stream()
                .filter(d -> d.getSpeed() != null)
                .mapToInt(d -> d.getSpeed().doubleValue() > 120 ? 1 : 0).sum();
        score -= speedingCount * 2;
        
        // 基于严重事件扣分
        long criticalEvents = events.stream().mapToInt(e -> Event.Severity.CRITICAL.equals(e.getSeverity()) ? 1 : 0).sum();
        score -= criticalEvents * 20;
        
        return Math.max(0, score);
    }
}
