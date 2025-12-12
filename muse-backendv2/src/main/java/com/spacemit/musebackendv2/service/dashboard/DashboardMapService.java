package com.spacemit.musebackendv2.service.dashboard;

import com.spacemit.musebackendv2.dto.dashboard.*;
import com.spacemit.musebackendv2.entity.v2.*;
import com.spacemit.musebackendv2.repository.v2.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 地图数据服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardMapService {

    private final DeviceV2Repository deviceRepository;
    private final DriverV2Repository driverRepository;
    private final DeviceDriverBindingV2Repository bindingRepository;
    private final GpsDataV2Repository gpsDataRepository;
    private final StatusDataV2Repository statusDataRepository;
    private final EventDataV2Repository eventDataRepository;

    /**
     * 获取车辆实时位置
     */
    public Map<String, Object> getMapVehicles(String bounds) {
        // 解析地图边界（格式：north,south,east,west）
        Double north = null, south = null, east = null, west = null;
        if (bounds != null && !bounds.isEmpty()) {
            String[] parts = bounds.split(",");
            if (parts.length == 4) {
                try {
                    north = Double.parseDouble(parts[0]);
                    south = Double.parseDouble(parts[1]);
                    east = Double.parseDouble(parts[2]);
                    west = Double.parseDouble(parts[3]);
                } catch (NumberFormatException e) {
                    log.warn("地图边界参数格式错误: {}", bounds);
                }
            }
        }
        
        // 获取所有在线设备
        List<DeviceV2> devices = deviceRepository.findByStatus(DeviceV2.DeviceStatus.ONLINE);
        
        List<MapVehicleDTO> vehicles = new ArrayList<>();
        
        for (DeviceV2 device : devices) {
            // 获取最新GPS数据
            Optional<GpsDataV2> latestGpsOpt = gpsDataRepository.findLatestByDeviceId(device.getDeviceId());
            if (!latestGpsOpt.isPresent()) {
                continue; // 没有GPS数据，跳过
            }
            
            GpsDataV2 latestGps = latestGpsOpt.get();
            if (latestGps.getLocationLat() == null || latestGps.getLocationLng() == null) {
                log.debug("设备{}最新GPS缺少坐标，跳过地图展示", device.getDeviceId());
                continue; // 坐标为空无法绘制，跳过
            }
            
            // 如果指定了边界，检查是否在边界内
            if (north != null && south != null && east != null && west != null) {
                if (latestGps.getLocationLat() == null || latestGps.getLocationLng() == null) {
                    log.debug("设备{}最新GPS缺少坐标，无法进行边界过滤", device.getDeviceId());
                    continue;
                }
                double lat = latestGps.getLocationLat().doubleValue();
                double lng = latestGps.getLocationLng().doubleValue();
                if (lat < south || lat > north || lng < west || lng > east) {
                    continue; // 不在边界内，跳过
                }
            }
            
            // 获取驾驶员信息
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
            String level = "Normal";
            Double score = 0.0;
            String behavior = "focused";
            Optional<StatusDataV2> latestStatusOpt = statusDataRepository.findLatestOneByDeviceId(device.getDeviceId());
            if (latestStatusOpt.isPresent()) {
                StatusDataV2 latestStatus = latestStatusOpt.get();
                if (latestStatus.getLevel() != null) {
                    level = latestStatus.getLevel();
                }
                if (latestStatus.getScore() != null) {
                    score = latestStatus.getScore().doubleValue();
                }
            }
            
            // 构建位置信息
            LocationDTO location = new LocationDTO();
            location.setLat(latestGps.getLocationLat());
            location.setLng(latestGps.getLocationLng());
            location.setSpeed(latestGps.getSpeed());
            location.setHeading(latestGps.getDirection());
            location.setAltitude(latestGps.getAltitude());
            
            // 构建状态信息
            VehicleStatusDTO status = new VehicleStatusDTO();
            status.setCurrentLevel(level);
            status.setCurrentScore(score);
            status.setStatus("online");
            
            MapVehicleDTO vehicle = new MapVehicleDTO();
            vehicle.setDeviceId(device.getDeviceId());
            vehicle.setDriverId(driverId);
            vehicle.setDriverName(driverName);
            vehicle.setLocation(location);
            vehicle.setStatus(status);
            vehicle.setLastUpdateTime(latestGps.getTimestamp());
            
            vehicles.add(vehicle);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("vehicles", vehicles);
        if (north != null && south != null && east != null && west != null) {
            Map<String, Double> boundsMap = new HashMap<>();
            boundsMap.put("north", north);
            boundsMap.put("south", south);
            boundsMap.put("east", east);
            boundsMap.put("west", west);
            response.put("bounds", boundsMap);
        }
        
        return response;
    }

    /**
     * 获取车辆轨迹回放
     */
    public TrackResponse getTrack(String deviceId, Long startTime, Long endTime) {
        // 获取GPS轨迹数据
        List<GpsDataV2> gpsTrack = gpsDataRepository.findTrackByDeviceIdAndTimeRange(deviceId, startTime, endTime);
        
        if (gpsTrack.isEmpty()) {
            throw new IllegalArgumentException("未找到轨迹数据");
        }
        
        // 获取设备信息
        Optional<DeviceV2> deviceOpt = deviceRepository.findByDeviceId(deviceId);
        if (!deviceOpt.isPresent()) {
            throw new IllegalArgumentException("设备不存在");
        }
        
        // 获取驾驶员信息
        String driverId = null;
        String driverName = null;
        Optional<DeviceDriverBindingV2> bindingOpt = bindingRepository.findActiveBindingByDeviceId(deviceId);
        if (bindingOpt.isPresent()) {
            driverId = bindingOpt.get().getDriverId();
            Optional<DriverV2> driverOpt = driverRepository.findByDriverId(driverId);
            if (driverOpt.isPresent()) {
                driverName = driverOpt.get().getDriverName();
            }
        }
        
        // 获取行程ID（如果有）
        String tripId = gpsTrack.stream()
            .filter(g -> g.getTripId() != null)
            .map(GpsDataV2::getTripId)
            .findFirst()
            .orElse(null);
        
        // 获取该时间段内的事件
        List<EventDataV2> events = eventDataRepository.findByDeviceIdAndTimestampBetween(deviceId, startTime, endTime);
        
        // 去重GPS数据（相同timestamp只保留一个）
        Map<Long, GpsDataV2> gpsMap = new LinkedHashMap<>();
        for (GpsDataV2 gps : gpsTrack) {
            // 如果已存在相同timestamp的GPS点，保留第一个（或可以合并数据）
            gpsMap.putIfAbsent(gps.getTimestamp(), gps);
        }
        List<GpsDataV2> uniqueGpsTrack = new ArrayList<>(gpsMap.values());
        
        // 构建轨迹点（按时间顺序）
        List<TrackPointDTO> track = new ArrayList<>();
        
        // 将事件按时间分组，并匹配到最接近的GPS点（允许时间差在5秒内）
        Map<Long, List<EventDataV2>> eventsByGpsTime = new HashMap<>();
        for (EventDataV2 event : events) {
            // 找到最接近的GPS点
            GpsDataV2 closestGps = null;
            long minTimeDiff = Long.MAX_VALUE;
            for (GpsDataV2 gps : uniqueGpsTrack) {
                long timeDiff = Math.abs(event.getTimestamp() - gps.getTimestamp());
                if (timeDiff < minTimeDiff && timeDiff <= 5 * 1000) { // 5秒内
                    minTimeDiff = timeDiff;
                    closestGps = gps;
                }
            }
            if (closestGps != null) {
                eventsByGpsTime.computeIfAbsent(closestGps.getTimestamp(), k -> new ArrayList<>()).add(event);
            }
        }
        
        // 获取状态数据（用于疲劳度）
        List<StatusDataV2> statusList = statusDataRepository.findByDeviceIdAndTimestampBetween(deviceId, startTime, endTime);
        Map<Long, StatusDataV2> statusByTime = statusList.stream()
            .collect(Collectors.toMap(StatusDataV2::getTimestamp, s -> s, (s1, s2) -> s1));
        
        for (GpsDataV2 gps : uniqueGpsTrack) {
            LocationDTO location = new LocationDTO();
            location.setLat(gps.getLocationLat());
            location.setLng(gps.getLocationLng());
            location.setSpeed(gps.getSpeed());
            location.setHeading(gps.getDirection());
            location.setAltitude(gps.getAltitude());
            
            // 获取该时间点的状态数据（查找最接近的状态，30秒内）
            String level = "Normal";
            Double score = 0.0;
            StatusDataV2 closestStatus = null;
            long minStatusTimeDiff = Long.MAX_VALUE;
            for (StatusDataV2 status : statusList) {
                long timeDiff = Math.abs(status.getTimestamp() - gps.getTimestamp());
                if (timeDiff < minStatusTimeDiff && timeDiff <= 30 * 1000) { // 30秒内
                    minStatusTimeDiff = timeDiff;
                    closestStatus = status;
                }
            }
            if (closestStatus != null) {
                level = closestStatus.getLevel();
                score = closestStatus.getScore().doubleValue();
            }
            
            TrackPointDTO.FatigueDTO fatigue = new TrackPointDTO.FatigueDTO();
            fatigue.setScore(score);
            fatigue.setLevel(level);
            
            // 获取附加到该GPS点的事件
            List<TrackPointDTO.EventMarkerDTO> eventMarkers = new ArrayList<>();
            List<EventDataV2> pointEvents = eventsByGpsTime.getOrDefault(gps.getTimestamp(), Collections.emptyList());
            for (EventDataV2 event : pointEvents) {
                TrackPointDTO.EventMarkerDTO marker = new TrackPointDTO.EventMarkerDTO();
                marker.setEventId(event.getEventId());
                marker.setLevel(event.getLevel());
                marker.setBehavior(event.getBehavior());
                marker.setTimestamp(event.getTimestamp());
                marker.setScore(event.getScore());
                marker.setAddress(event.getLocationAddress());
                eventMarkers.add(marker);
            }
            
            TrackPointDTO point = new TrackPointDTO();
            point.setTimestamp(gps.getTimestamp());
            point.setLocation(location);
            point.setFatigue(fatigue);
            point.setEvents(eventMarkers);
            
            track.add(point);
        }
        
        // 计算总距离（简化计算，使用GPS点之间的距离累加）
        BigDecimal totalDistance = calculateTotalDistance(uniqueGpsTrack);
        
        // 计算总时长（实际GPS数据的时间跨度）
        int totalDuration = 0;
        if (!uniqueGpsTrack.isEmpty()) {
            long actualStartTime = uniqueGpsTrack.get(0).getTimestamp();
            long actualEndTime = uniqueGpsTrack.get(uniqueGpsTrack.size() - 1).getTimestamp();
            totalDuration = (int) ((actualEndTime - actualStartTime) / 1000);
        }
        
        // 不再单独返回events列表，因为事件已经附加到track中的每个GPS点上了
        
        // 计算统计信息
        TrackResponse.TrackStatisticsDTO statistics = new TrackResponse.TrackStatisticsDTO();
        statistics.setTotalEvents(events.size());
        statistics.setCriticalEvents((int) events.stream().filter(e -> "Level 3".equals(e.getLevel())).count());
        statistics.setHighEvents((int) events.stream().filter(e -> "Level 2".equals(e.getLevel())).count());
        statistics.setMediumEvents((int) events.stream().filter(e -> "Level 1".equals(e.getLevel())).count());
        statistics.setLowEvents((int) events.stream().filter(e -> "Normal".equals(e.getLevel())).count());
        
        OptionalDouble avgScoreOpt = events.stream()
            .filter(e -> e.getScore() != null)
            .mapToDouble(e -> e.getScore().doubleValue())
            .average();
        statistics.setAvgScore(avgScoreOpt.isPresent() ? 
            BigDecimal.valueOf(avgScoreOpt.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO);
        
        OptionalDouble maxScoreOpt = events.stream()
            .filter(e -> e.getScore() != null)
            .mapToDouble(e -> e.getScore().doubleValue())
            .max();
        statistics.setMaxScore(maxScoreOpt.isPresent() ? 
            BigDecimal.valueOf(maxScoreOpt.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO);
        
        TrackResponse response = new TrackResponse();
        response.setDeviceId(deviceId);
        response.setDriverId(driverId);
        response.setDriverName(driverName);
        response.setTripId(tripId);
        response.setStartTime(startTime);
        response.setEndTime(endTime);
        response.setTotalDistance(totalDistance);
        response.setTotalDuration(totalDuration);
        response.setTrack(track);
        response.setEvents(Collections.emptyList()); // 不再单独返回events，已附加到track中
        response.setStatistics(statistics);
        
        return response;
    }

    /**
     * 计算总距离（使用Haversine公式）
     */
    private BigDecimal calculateTotalDistance(List<GpsDataV2> gpsTrack) {
        if (gpsTrack.size() < 2) {
            return BigDecimal.ZERO;
        }
        
        double totalDistance = 0.0;
        for (int i = 1; i < gpsTrack.size(); i++) {
            GpsDataV2 prev = gpsTrack.get(i - 1);
            GpsDataV2 curr = gpsTrack.get(i);
            
            if (prev.getLocationLat() != null && prev.getLocationLng() != null &&
                curr.getLocationLat() != null && curr.getLocationLng() != null) {
                double distance = haversineDistance(
                    prev.getLocationLat().doubleValue(),
                    prev.getLocationLng().doubleValue(),
                    curr.getLocationLat().doubleValue(),
                    curr.getLocationLng().doubleValue()
                );
                totalDistance += distance;
            }
        }
        
        return BigDecimal.valueOf(totalDistance).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Haversine公式计算两点间距离（公里）
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球半径（公里）
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * 获取疲劳事件热力图
     */
    public HeatmapResponse getHeatmap(Long startTime, Long endTime, String level) {
        // 获取事件数据
        List<EventDataV2> events = eventDataRepository.findByTimestampBetween(startTime, endTime);
        
        // 如果指定了级别，进行筛选
        if (level != null && !level.isEmpty() && !"all".equalsIgnoreCase(level)) {
            final String filterLevel = level;
            events = events.stream()
                .filter(e -> filterLevel.equals(e.getLevel()))
                .collect(Collectors.toList());
        }
        
        if (events.isEmpty()) {
            HeatmapResponse response = new HeatmapResponse();
            response.setLevel(level);
            response.setStartTime(startTime);
            response.setEndTime(endTime);
            response.setPoints(Collections.emptyList());
            response.setStatistics(new HeatmapResponse.HeatmapStatisticsDTO());
            return response;
        }
        
        // 坐标聚合（将相近的坐标点聚合到一起）
        // 使用网格聚合：将坐标按0.01度（约1公里）的网格分组
        Map<String, List<EventDataV2>> gridMap = new HashMap<>();
        double gridSize = 0.01; // 约1公里
        
        for (EventDataV2 event : events) {
            if (event.getLocationLat() != null && event.getLocationLng() != null) {
                double lat = event.getLocationLat().doubleValue();
                double lng = event.getLocationLng().doubleValue();
                
                // 计算网格坐标
                int gridLat = (int) (lat / gridSize);
                int gridLng = (int) (lng / gridSize);
                String gridKey = gridLat + "_" + gridLng;
                
                gridMap.computeIfAbsent(gridKey, k -> new ArrayList<>()).add(event);
            }
        }
        
        // 构建热力图点
        List<HeatmapResponse.HeatmapPointDTO> points = new ArrayList<>();
        int maxEventCount = 0;
        double maxIntensity = 0.0;
        double minIntensity = 1.0;
        
        for (Map.Entry<String, List<EventDataV2>> entry : gridMap.entrySet()) {
            List<EventDataV2> gridEvents = entry.getValue();
            int eventCount = gridEvents.size();
            
            // 计算网格中心点
            OptionalDouble avgLatOpt = gridEvents.stream()
                .filter(e -> e.getLocationLat() != null)
                .mapToDouble(e -> e.getLocationLat().doubleValue())
                .average();
            OptionalDouble avgLngOpt = gridEvents.stream()
                .filter(e -> e.getLocationLng() != null)
                .mapToDouble(e -> e.getLocationLng().doubleValue())
                .average();
            
            if (!avgLatOpt.isPresent() || !avgLngOpt.isPresent()) {
                continue;
            }
            
            BigDecimal centerLat = BigDecimal.valueOf(avgLatOpt.getAsDouble()).setScale(6, RoundingMode.HALF_UP);
            BigDecimal centerLng = BigDecimal.valueOf(avgLngOpt.getAsDouble()).setScale(6, RoundingMode.HALF_UP);
            
            // 计算强度（基于事件数量，归一化到0-1）
            // 强度 = 事件数 / 最大事件数（在本次查询中）
            maxEventCount = Math.max(maxEventCount, eventCount);
            
            // 计算分数统计
            OptionalDouble maxScoreOpt = gridEvents.stream()
                .filter(e -> e.getScore() != null)
                .mapToDouble(e -> e.getScore().doubleValue())
                .max();
            OptionalDouble avgScoreOpt = gridEvents.stream()
                .filter(e -> e.getScore() != null)
                .mapToDouble(e -> e.getScore().doubleValue())
                .average();
            
            BigDecimal maxScore = maxScoreOpt.isPresent() ? 
                BigDecimal.valueOf(maxScoreOpt.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
            BigDecimal avgScore = avgScoreOpt.isPresent() ? 
                BigDecimal.valueOf(avgScoreOpt.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
            
            HeatmapResponse.HeatmapPointDTO point = new HeatmapResponse.HeatmapPointDTO();
            point.setLat(centerLat);
            point.setLng(centerLng);
            point.setEventCount(eventCount);
            point.setMaxScore(maxScore);
            point.setAvgScore(avgScore);
            // 强度暂时设为0，后面会统一计算
            point.setIntensity(0.0);
            
            points.add(point);
        }
        
        // 计算强度（归一化）
        if (maxEventCount > 0) {
            for (HeatmapResponse.HeatmapPointDTO point : points) {
                double intensity = point.getEventCount() / (double) maxEventCount;
                point.setIntensity(intensity);
                maxIntensity = Math.max(maxIntensity, intensity);
                minIntensity = Math.min(minIntensity, intensity);
            }
        }
        
        // 计算边界
        HeatmapResponse.MapBoundsDTO bounds = calculateBounds(events);
        
        // 统计信息
        HeatmapResponse.HeatmapStatisticsDTO statistics = new HeatmapResponse.HeatmapStatisticsDTO();
        statistics.setTotalPoints(points.size());
        statistics.setMaxIntensity(maxIntensity);
        statistics.setMinIntensity(minIntensity);
        statistics.setTotalEvents(events.size());
        
        HeatmapResponse response = new HeatmapResponse();
        response.setLevel(level);
        response.setStartTime(startTime);
        response.setEndTime(endTime);
        response.setPoints(points);
        response.setBounds(bounds);
        response.setStatistics(statistics);
        
        return response;
    }

    /**
     * 计算事件边界
     */
    private HeatmapResponse.MapBoundsDTO calculateBounds(List<EventDataV2> events) {
        if (events.isEmpty()) {
            return new HeatmapResponse.MapBoundsDTO(0.0, 0.0, 0.0, 0.0);
        }
        
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLng = Double.MAX_VALUE;
        double maxLng = -Double.MAX_VALUE;
        
        for (EventDataV2 event : events) {
            if (event.getLocationLat() != null && event.getLocationLng() != null) {
                double lat = event.getLocationLat().doubleValue();
                double lng = event.getLocationLng().doubleValue();
                
                minLat = Math.min(minLat, lat);
                maxLat = Math.max(maxLat, lat);
                minLng = Math.min(minLng, lng);
                maxLng = Math.max(maxLng, lng);
            }
        }
        
        // 扩展边界（增加10%的边距）
        double latRange = maxLat - minLat;
        double lngRange = maxLng - minLng;
        
        return new HeatmapResponse.MapBoundsDTO(
            maxLat + latRange * 0.1,
            minLat - latRange * 0.1,
            maxLng + lngRange * 0.1,
            minLng - lngRange * 0.1
        );
    }
}


import com.spacemit.musebackendv2.dto.dashboard.*;
import com.spacemit.musebackendv2.entity.v2.*;
import com.spacemit.musebackendv2.repository.v2.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 地图数据服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardMapService {

    private final DeviceV2Repository deviceRepository;
    private final DriverV2Repository driverRepository;
    private final DeviceDriverBindingV2Repository bindingRepository;
    private final GpsDataV2Repository gpsDataRepository;
    private final StatusDataV2Repository statusDataRepository;
    private final EventDataV2Repository eventDataRepository;

    /**
     * 获取车辆实时位置
     */
    public Map<String, Object> getMapVehicles(String bounds) {
        // 解析地图边界（格式：north,south,east,west）
        Double north = null, south = null, east = null, west = null;
        if (bounds != null && !bounds.isEmpty()) {
            String[] parts = bounds.split(",");
            if (parts.length == 4) {
                try {
                    north = Double.parseDouble(parts[0]);
                    south = Double.parseDouble(parts[1]);
                    east = Double.parseDouble(parts[2]);
                    west = Double.parseDouble(parts[3]);
                } catch (NumberFormatException e) {
                    log.warn("地图边界参数格式错误: {}", bounds);
                }
            }
        }
        
        // 获取所有在线设备
        List<DeviceV2> devices = deviceRepository.findByStatus(DeviceV2.DeviceStatus.ONLINE);
        
        List<MapVehicleDTO> vehicles = new ArrayList<>();
        
        for (DeviceV2 device : devices) {
            // 获取最新GPS数据
            Optional<GpsDataV2> latestGpsOpt = gpsDataRepository.findLatestByDeviceId(device.getDeviceId());
            if (!latestGpsOpt.isPresent()) {
                continue; // 没有GPS数据，跳过
            }
            
            GpsDataV2 latestGps = latestGpsOpt.get();
            if (latestGps.getLocationLat() == null || latestGps.getLocationLng() == null) {
                log.debug("设备{}最新GPS缺少坐标，跳过地图展示", device.getDeviceId());
                continue; // 坐标为空无法绘制，跳过
            }
            
            // 如果指定了边界，检查是否在边界内
            if (north != null && south != null && east != null && west != null) {
                if (latestGps.getLocationLat() == null || latestGps.getLocationLng() == null) {
                    log.debug("设备{}最新GPS缺少坐标，无法进行边界过滤", device.getDeviceId());
                    continue;
                }
                double lat = latestGps.getLocationLat().doubleValue();
                double lng = latestGps.getLocationLng().doubleValue();
                if (lat < south || lat > north || lng < west || lng > east) {
                    continue; // 不在边界内，跳过
                }
            }
            
            // 获取驾驶员信息
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
            String level = "Normal";
            Double score = 0.0;
            String behavior = "focused";
            Optional<StatusDataV2> latestStatusOpt = statusDataRepository.findLatestOneByDeviceId(device.getDeviceId());
            if (latestStatusOpt.isPresent()) {
                StatusDataV2 latestStatus = latestStatusOpt.get();
                if (latestStatus.getLevel() != null) {
                    level = latestStatus.getLevel();
                }
                if (latestStatus.getScore() != null) {
                    score = latestStatus.getScore().doubleValue();
                }
            }
            
            // 构建位置信息
            LocationDTO location = new LocationDTO();
            location.setLat(latestGps.getLocationLat());
            location.setLng(latestGps.getLocationLng());
            location.setSpeed(latestGps.getSpeed());
            location.setHeading(latestGps.getDirection());
            location.setAltitude(latestGps.getAltitude());
            
            // 构建状态信息
            VehicleStatusDTO status = new VehicleStatusDTO();
            status.setCurrentLevel(level);
            status.setCurrentScore(score);
            status.setStatus("online");
            
            MapVehicleDTO vehicle = new MapVehicleDTO();
            vehicle.setDeviceId(device.getDeviceId());
            vehicle.setDriverId(driverId);
            vehicle.setDriverName(driverName);
            vehicle.setLocation(location);
            vehicle.setStatus(status);
            vehicle.setLastUpdateTime(latestGps.getTimestamp());
            
            vehicles.add(vehicle);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("vehicles", vehicles);
        if (north != null && south != null && east != null && west != null) {
            Map<String, Double> boundsMap = new HashMap<>();
            boundsMap.put("north", north);
            boundsMap.put("south", south);
            boundsMap.put("east", east);
            boundsMap.put("west", west);
            response.put("bounds", boundsMap);
        }
        
        return response;
    }

    /**
     * 获取车辆轨迹回放
     */
    public TrackResponse getTrack(String deviceId, Long startTime, Long endTime) {
        // 获取GPS轨迹数据
        List<GpsDataV2> gpsTrack = gpsDataRepository.findTrackByDeviceIdAndTimeRange(deviceId, startTime, endTime);
        
        if (gpsTrack.isEmpty()) {
            throw new IllegalArgumentException("未找到轨迹数据");
        }
        
        // 获取设备信息
        Optional<DeviceV2> deviceOpt = deviceRepository.findByDeviceId(deviceId);
        if (!deviceOpt.isPresent()) {
            throw new IllegalArgumentException("设备不存在");
        }
        
        // 获取驾驶员信息
        String driverId = null;
        String driverName = null;
        Optional<DeviceDriverBindingV2> bindingOpt = bindingRepository.findActiveBindingByDeviceId(deviceId);
        if (bindingOpt.isPresent()) {
            driverId = bindingOpt.get().getDriverId();
            Optional<DriverV2> driverOpt = driverRepository.findByDriverId(driverId);
            if (driverOpt.isPresent()) {
                driverName = driverOpt.get().getDriverName();
            }
        }
        
        // 获取行程ID（如果有）
        String tripId = gpsTrack.stream()
            .filter(g -> g.getTripId() != null)
            .map(GpsDataV2::getTripId)
            .findFirst()
            .orElse(null);
        
        // 获取该时间段内的事件
        List<EventDataV2> events = eventDataRepository.findByDeviceIdAndTimestampBetween(deviceId, startTime, endTime);
        
        // 去重GPS数据（相同timestamp只保留一个）
        Map<Long, GpsDataV2> gpsMap = new LinkedHashMap<>();
        for (GpsDataV2 gps : gpsTrack) {
            // 如果已存在相同timestamp的GPS点，保留第一个（或可以合并数据）
            gpsMap.putIfAbsent(gps.getTimestamp(), gps);
        }
        List<GpsDataV2> uniqueGpsTrack = new ArrayList<>(gpsMap.values());
        
        // 构建轨迹点（按时间顺序）
        List<TrackPointDTO> track = new ArrayList<>();
        
        // 将事件按时间分组，并匹配到最接近的GPS点（允许时间差在5秒内）
        Map<Long, List<EventDataV2>> eventsByGpsTime = new HashMap<>();
        for (EventDataV2 event : events) {
            // 找到最接近的GPS点
            GpsDataV2 closestGps = null;
            long minTimeDiff = Long.MAX_VALUE;
            for (GpsDataV2 gps : uniqueGpsTrack) {
                long timeDiff = Math.abs(event.getTimestamp() - gps.getTimestamp());
                if (timeDiff < minTimeDiff && timeDiff <= 5 * 1000) { // 5秒内
                    minTimeDiff = timeDiff;
                    closestGps = gps;
                }
            }
            if (closestGps != null) {
                eventsByGpsTime.computeIfAbsent(closestGps.getTimestamp(), k -> new ArrayList<>()).add(event);
            }
        }
        
        // 获取状态数据（用于疲劳度）
        List<StatusDataV2> statusList = statusDataRepository.findByDeviceIdAndTimestampBetween(deviceId, startTime, endTime);
        Map<Long, StatusDataV2> statusByTime = statusList.stream()
            .collect(Collectors.toMap(StatusDataV2::getTimestamp, s -> s, (s1, s2) -> s1));
        
        for (GpsDataV2 gps : uniqueGpsTrack) {
            LocationDTO location = new LocationDTO();
            location.setLat(gps.getLocationLat());
            location.setLng(gps.getLocationLng());
            location.setSpeed(gps.getSpeed());
            location.setHeading(gps.getDirection());
            location.setAltitude(gps.getAltitude());
            
            // 获取该时间点的状态数据（查找最接近的状态，30秒内）
            String level = "Normal";
            Double score = 0.0;
            StatusDataV2 closestStatus = null;
            long minStatusTimeDiff = Long.MAX_VALUE;
            for (StatusDataV2 status : statusList) {
                long timeDiff = Math.abs(status.getTimestamp() - gps.getTimestamp());
                if (timeDiff < minStatusTimeDiff && timeDiff <= 30 * 1000) { // 30秒内
                    minStatusTimeDiff = timeDiff;
                    closestStatus = status;
                }
            }
            if (closestStatus != null) {
                level = closestStatus.getLevel();
                score = closestStatus.getScore().doubleValue();
            }
            
            TrackPointDTO.FatigueDTO fatigue = new TrackPointDTO.FatigueDTO();
            fatigue.setScore(score);
            fatigue.setLevel(level);
            
            // 获取附加到该GPS点的事件
            List<TrackPointDTO.EventMarkerDTO> eventMarkers = new ArrayList<>();
            List<EventDataV2> pointEvents = eventsByGpsTime.getOrDefault(gps.getTimestamp(), Collections.emptyList());
            for (EventDataV2 event : pointEvents) {
                TrackPointDTO.EventMarkerDTO marker = new TrackPointDTO.EventMarkerDTO();
                marker.setEventId(event.getEventId());
                marker.setLevel(event.getLevel());
                marker.setBehavior(event.getBehavior());
                marker.setTimestamp(event.getTimestamp());
                marker.setScore(event.getScore());
                marker.setAddress(event.getLocationAddress());
                eventMarkers.add(marker);
            }
            
            TrackPointDTO point = new TrackPointDTO();
            point.setTimestamp(gps.getTimestamp());
            point.setLocation(location);
            point.setFatigue(fatigue);
            point.setEvents(eventMarkers);
            
            track.add(point);
        }
        
        // 计算总距离（简化计算，使用GPS点之间的距离累加）
        BigDecimal totalDistance = calculateTotalDistance(uniqueGpsTrack);
        
        // 计算总时长（实际GPS数据的时间跨度）
        int totalDuration = 0;
        if (!uniqueGpsTrack.isEmpty()) {
            long actualStartTime = uniqueGpsTrack.get(0).getTimestamp();
            long actualEndTime = uniqueGpsTrack.get(uniqueGpsTrack.size() - 1).getTimestamp();
            totalDuration = (int) ((actualEndTime - actualStartTime) / 1000);
        }
        
        // 不再单独返回events列表，因为事件已经附加到track中的每个GPS点上了
        
        // 计算统计信息
        TrackResponse.TrackStatisticsDTO statistics = new TrackResponse.TrackStatisticsDTO();
        statistics.setTotalEvents(events.size());
        statistics.setCriticalEvents((int) events.stream().filter(e -> "Level 3".equals(e.getLevel())).count());
        statistics.setHighEvents((int) events.stream().filter(e -> "Level 2".equals(e.getLevel())).count());
        statistics.setMediumEvents((int) events.stream().filter(e -> "Level 1".equals(e.getLevel())).count());
        statistics.setLowEvents((int) events.stream().filter(e -> "Normal".equals(e.getLevel())).count());
        
        OptionalDouble avgScoreOpt = events.stream()
            .filter(e -> e.getScore() != null)
            .mapToDouble(e -> e.getScore().doubleValue())
            .average();
        statistics.setAvgScore(avgScoreOpt.isPresent() ? 
            BigDecimal.valueOf(avgScoreOpt.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO);
        
        OptionalDouble maxScoreOpt = events.stream()
            .filter(e -> e.getScore() != null)
            .mapToDouble(e -> e.getScore().doubleValue())
            .max();
        statistics.setMaxScore(maxScoreOpt.isPresent() ? 
            BigDecimal.valueOf(maxScoreOpt.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO);
        
        TrackResponse response = new TrackResponse();
        response.setDeviceId(deviceId);
        response.setDriverId(driverId);
        response.setDriverName(driverName);
        response.setTripId(tripId);
        response.setStartTime(startTime);
        response.setEndTime(endTime);
        response.setTotalDistance(totalDistance);
        response.setTotalDuration(totalDuration);
        response.setTrack(track);
        response.setEvents(Collections.emptyList()); // 不再单独返回events，已附加到track中
        response.setStatistics(statistics);
        
        return response;
    }

    /**
     * 计算总距离（使用Haversine公式）
     */
    private BigDecimal calculateTotalDistance(List<GpsDataV2> gpsTrack) {
        if (gpsTrack.size() < 2) {
            return BigDecimal.ZERO;
        }
        
        double totalDistance = 0.0;
        for (int i = 1; i < gpsTrack.size(); i++) {
            GpsDataV2 prev = gpsTrack.get(i - 1);
            GpsDataV2 curr = gpsTrack.get(i);
            
            if (prev.getLocationLat() != null && prev.getLocationLng() != null &&
                curr.getLocationLat() != null && curr.getLocationLng() != null) {
                double distance = haversineDistance(
                    prev.getLocationLat().doubleValue(),
                    prev.getLocationLng().doubleValue(),
                    curr.getLocationLat().doubleValue(),
                    curr.getLocationLng().doubleValue()
                );
                totalDistance += distance;
            }
        }
        
        return BigDecimal.valueOf(totalDistance).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Haversine公式计算两点间距离（公里）
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球半径（公里）
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * 获取疲劳事件热力图
     */
    public HeatmapResponse getHeatmap(Long startTime, Long endTime, String level) {
        // 获取事件数据
        List<EventDataV2> events = eventDataRepository.findByTimestampBetween(startTime, endTime);
        
        // 如果指定了级别，进行筛选
        if (level != null && !level.isEmpty() && !"all".equalsIgnoreCase(level)) {
            final String filterLevel = level;
            events = events.stream()
                .filter(e -> filterLevel.equals(e.getLevel()))
                .collect(Collectors.toList());
        }
        
        if (events.isEmpty()) {
            HeatmapResponse response = new HeatmapResponse();
            response.setLevel(level);
            response.setStartTime(startTime);
            response.setEndTime(endTime);
            response.setPoints(Collections.emptyList());
            response.setStatistics(new HeatmapResponse.HeatmapStatisticsDTO());
            return response;
        }
        
        // 坐标聚合（将相近的坐标点聚合到一起）
        // 使用网格聚合：将坐标按0.01度（约1公里）的网格分组
        Map<String, List<EventDataV2>> gridMap = new HashMap<>();
        double gridSize = 0.01; // 约1公里
        
        for (EventDataV2 event : events) {
            if (event.getLocationLat() != null && event.getLocationLng() != null) {
                double lat = event.getLocationLat().doubleValue();
                double lng = event.getLocationLng().doubleValue();
                
                // 计算网格坐标
                int gridLat = (int) (lat / gridSize);
                int gridLng = (int) (lng / gridSize);
                String gridKey = gridLat + "_" + gridLng;
                
                gridMap.computeIfAbsent(gridKey, k -> new ArrayList<>()).add(event);
            }
        }
        
        // 构建热力图点
        List<HeatmapResponse.HeatmapPointDTO> points = new ArrayList<>();
        int maxEventCount = 0;
        double maxIntensity = 0.0;
        double minIntensity = 1.0;
        
        for (Map.Entry<String, List<EventDataV2>> entry : gridMap.entrySet()) {
            List<EventDataV2> gridEvents = entry.getValue();
            int eventCount = gridEvents.size();
            
            // 计算网格中心点
            OptionalDouble avgLatOpt = gridEvents.stream()
                .filter(e -> e.getLocationLat() != null)
                .mapToDouble(e -> e.getLocationLat().doubleValue())
                .average();
            OptionalDouble avgLngOpt = gridEvents.stream()
                .filter(e -> e.getLocationLng() != null)
                .mapToDouble(e -> e.getLocationLng().doubleValue())
                .average();
            
            if (!avgLatOpt.isPresent() || !avgLngOpt.isPresent()) {
                continue;
            }
            
            BigDecimal centerLat = BigDecimal.valueOf(avgLatOpt.getAsDouble()).setScale(6, RoundingMode.HALF_UP);
            BigDecimal centerLng = BigDecimal.valueOf(avgLngOpt.getAsDouble()).setScale(6, RoundingMode.HALF_UP);
            
            // 计算强度（基于事件数量，归一化到0-1）
            // 强度 = 事件数 / 最大事件数（在本次查询中）
            maxEventCount = Math.max(maxEventCount, eventCount);
            
            // 计算分数统计
            OptionalDouble maxScoreOpt = gridEvents.stream()
                .filter(e -> e.getScore() != null)
                .mapToDouble(e -> e.getScore().doubleValue())
                .max();
            OptionalDouble avgScoreOpt = gridEvents.stream()
                .filter(e -> e.getScore() != null)
                .mapToDouble(e -> e.getScore().doubleValue())
                .average();
            
            BigDecimal maxScore = maxScoreOpt.isPresent() ? 
                BigDecimal.valueOf(maxScoreOpt.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
            BigDecimal avgScore = avgScoreOpt.isPresent() ? 
                BigDecimal.valueOf(avgScoreOpt.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
            
            HeatmapResponse.HeatmapPointDTO point = new HeatmapResponse.HeatmapPointDTO();
            point.setLat(centerLat);
            point.setLng(centerLng);
            point.setEventCount(eventCount);
            point.setMaxScore(maxScore);
            point.setAvgScore(avgScore);
            // 强度暂时设为0，后面会统一计算
            point.setIntensity(0.0);
            
            points.add(point);
        }
        
        // 计算强度（归一化）
        if (maxEventCount > 0) {
            for (HeatmapResponse.HeatmapPointDTO point : points) {
                double intensity = point.getEventCount() / (double) maxEventCount;
                point.setIntensity(intensity);
                maxIntensity = Math.max(maxIntensity, intensity);
                minIntensity = Math.min(minIntensity, intensity);
            }
        }
        
        // 计算边界
        HeatmapResponse.MapBoundsDTO bounds = calculateBounds(events);
        
        // 统计信息
        HeatmapResponse.HeatmapStatisticsDTO statistics = new HeatmapResponse.HeatmapStatisticsDTO();
        statistics.setTotalPoints(points.size());
        statistics.setMaxIntensity(maxIntensity);
        statistics.setMinIntensity(minIntensity);
        statistics.setTotalEvents(events.size());
        
        HeatmapResponse response = new HeatmapResponse();
        response.setLevel(level);
        response.setStartTime(startTime);
        response.setEndTime(endTime);
        response.setPoints(points);
        response.setBounds(bounds);
        response.setStatistics(statistics);
        
        return response;
    }

    /**
     * 计算事件边界
     */
    private HeatmapResponse.MapBoundsDTO calculateBounds(List<EventDataV2> events) {
        if (events.isEmpty()) {
            return new HeatmapResponse.MapBoundsDTO(0.0, 0.0, 0.0, 0.0);
        }
        
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLng = Double.MAX_VALUE;
        double maxLng = -Double.MAX_VALUE;
        
        for (EventDataV2 event : events) {
            if (event.getLocationLat() != null && event.getLocationLng() != null) {
                double lat = event.getLocationLat().doubleValue();
                double lng = event.getLocationLng().doubleValue();
                
                minLat = Math.min(minLat, lat);
                maxLat = Math.max(maxLat, lat);
                minLng = Math.min(minLng, lng);
                maxLng = Math.max(maxLng, lng);
            }
        }
        
        // 扩展边界（增加10%的边距）
        double latRange = maxLat - minLat;
        double lngRange = maxLng - minLng;
        
        return new HeatmapResponse.MapBoundsDTO(
            maxLat + latRange * 0.1,
            minLat - latRange * 0.1,
            maxLng + lngRange * 0.1,
            minLng - lngRange * 0.1
        );
    }
}

