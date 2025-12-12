package com.spacemit.musebackendv2.service.dashboard;

import com.spacemit.musebackendv2.dto.dashboard.EventStatisticsResponse;
import com.spacemit.musebackendv2.dto.dashboard.RegionAnalysisResponse;
import com.spacemit.musebackendv2.dto.dashboard.TimeframeDataDTO;
import com.spacemit.musebackendv2.dto.dashboard.TimeframeResponse;
import com.spacemit.musebackendv2.entity.v2.EventDataV2;
import com.spacemit.musebackendv2.repository.v2.EventDataV2Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计数据服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardStatisticsService {

    private final EventDataV2Repository eventDataRepository;

    /**
     * 获取疲劳事件统计
     */
    public EventStatisticsResponse getEventStatistics(Long startTime, Long endTime, String driverId) {
        List<EventDataV2> events;
        
        if (driverId != null && !driverId.isEmpty()) {
            events = eventDataRepository.findByDriverIdAndTimestampBetween(driverId, startTime, endTime);
        } else {
            events = eventDataRepository.findByTimestampBetween(startTime, endTime);
        }
        
        // 总体统计
        EventStatisticsResponse.SummaryDTO summary = new EventStatisticsResponse.SummaryDTO();
        summary.setTotalEvents((long) events.size());
        summary.setCriticalEvents(events.stream().filter(e -> "Level 3".equals(e.getLevel())).count());
        summary.setHighEvents(events.stream().filter(e -> "Level 2".equals(e.getLevel())).count());
        summary.setMediumEvents(events.stream().filter(e -> "Level 1".equals(e.getLevel())).count());
        summary.setLowEvents(events.stream().filter(e -> "Normal".equals(e.getLevel())).count());
        
        // 按级别统计
        Map<String, Long> byLevel = events.stream()
            .collect(Collectors.groupingBy(
                EventDataV2::getLevel,
                Collectors.counting()
            ));
        
        // 按类型统计
        Map<String, Long> byType = events.stream()
            .filter(e -> e.getEventType() != null)
            .collect(Collectors.groupingBy(
                EventDataV2::getEventType,
                Collectors.counting()
            ));
        
        // 按行为统计
        Map<String, Long> byBehavior = events.stream()
            .filter(e -> e.getBehavior() != null)
            .collect(Collectors.groupingBy(
                EventDataV2::getBehavior,
                Collectors.counting()
            ));
        
        // 趋势统计
        EventStatisticsResponse.TrendDTO trend = calculateTrend();
        
        EventStatisticsResponse response = new EventStatisticsResponse();
        response.setSummary(summary);
        response.setByLevel(byLevel);
        response.setByType(byType);
        response.setByBehavior(byBehavior);
        response.setTrend(trend);
        
        return response;
    }

    /**
     * 获取时间段分析
     */
    public TimeframeResponse getTimeframeAnalysis(Long startTime, Long endTime, String interval) {
        List<EventDataV2> events = eventDataRepository.findByTimestampBetween(startTime, endTime);
        
        // 按时间间隔分组
        Map<String, List<EventDataV2>> groupedEvents = groupEventsByInterval(events, interval);
        
        List<TimeframeDataDTO> data = new ArrayList<>();
        SimpleDateFormat sdf = getDateFormat(interval);
        
        for (Map.Entry<String, List<EventDataV2>> entry : groupedEvents.entrySet()) {
            List<EventDataV2> groupEvents = entry.getValue();
            
            long eventCount = groupEvents.size();
            long criticalCount = groupEvents.stream().filter(e -> "Level 3".equals(e.getLevel())).count();
            long highCount = groupEvents.stream().filter(e -> "Level 2".equals(e.getLevel())).count();
            long mediumCount = groupEvents.stream().filter(e -> "Level 1".equals(e.getLevel())).count();
            long lowCount = groupEvents.stream().filter(e -> "Normal".equals(e.getLevel())).count();
            
            OptionalDouble avgScoreOpt = groupEvents.stream()
                .filter(e -> e.getScore() != null)
                .mapToDouble(e -> e.getScore().doubleValue())
                .average();
            BigDecimal avgScore = avgScoreOpt.isPresent() ? 
                BigDecimal.valueOf(avgScoreOpt.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
            
            OptionalDouble maxScoreOpt = groupEvents.stream()
                .filter(e -> e.getScore() != null)
                .mapToDouble(e -> e.getScore().doubleValue())
                .max();
            BigDecimal maxScore = maxScoreOpt.isPresent() ? 
                BigDecimal.valueOf(maxScoreOpt.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
            
            // 计算时间戳（使用该组的第一个事件时间）
            Long timestamp = groupEvents.isEmpty() ? startTime : groupEvents.get(0).getTimestamp();
            
            TimeframeDataDTO timeframeData = new TimeframeDataDTO();
            timeframeData.setTime(entry.getKey());
            timeframeData.setTimestamp(timestamp);
            timeframeData.setEventCount(eventCount);
            timeframeData.setCriticalCount(criticalCount);
            timeframeData.setHighCount(highCount);
            timeframeData.setMediumCount(mediumCount);
            timeframeData.setLowCount(lowCount);
            timeframeData.setAvgScore(avgScore);
            timeframeData.setMaxScore(maxScore);
            
            data.add(timeframeData);
        }
        
        // 按时间排序
        data.sort(Comparator.comparing(TimeframeDataDTO::getTimestamp));
        
        // 计算高峰时段
        List<TimeframeResponse.PeakHourDTO> peakHours = calculatePeakHours(data);
        
        // 统计信息
        TimeframeResponse.StatisticsDTO statistics = new TimeframeResponse.StatisticsDTO();
        statistics.setTotalEvents((long) events.size());
        if (!data.isEmpty()) {
            long totalHours = (endTime - startTime) / (60 * 60 * 1000);
            statistics.setAvgEventsPerHour(BigDecimal.valueOf(events.size() / Math.max(1.0, totalHours))
                .setScale(2, RoundingMode.HALF_UP));
            statistics.setMaxEventsInHour(data.stream().mapToLong(TimeframeDataDTO::getEventCount).max().orElse(0));
            statistics.setMinEventsInHour(data.stream().mapToLong(TimeframeDataDTO::getEventCount).min().orElse(0));
        }
        
        TimeframeResponse response = new TimeframeResponse();
        response.setInterval(interval);
        response.setData(data);
        response.setPeakHours(peakHours);
        response.setStatistics(statistics);
        
        return response;
    }

    /**
     * 按时间间隔分组事件
     */
    private Map<String, List<EventDataV2>> groupEventsByInterval(List<EventDataV2> events, String interval) {
        Map<String, List<EventDataV2>> grouped = new HashMap<>();
        SimpleDateFormat sdf = getDateFormat(interval);
        Calendar cal = Calendar.getInstance();
        
        for (EventDataV2 event : events) {
            cal.setTimeInMillis(event.getTimestamp());
            
            String key;
            switch (interval) {
                case "hour":
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    key = sdf.format(cal.getTime());
                    break;
                case "day":
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    key = sdf.format(cal.getTime());
                    break;
                default:
                    key = sdf.format(cal.getTime());
            }
            
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(event);
        }
        
        return grouped;
    }

    /**
     * 获取日期格式化器
     */
    private SimpleDateFormat getDateFormat(String interval) {
        switch (interval) {
            case "hour":
                return new SimpleDateFormat("yyyy-MM-dd HH:00:00");
            case "day":
                return new SimpleDateFormat("yyyy-MM-dd");
            case "week":
                return new SimpleDateFormat("yyyy-MM-dd");
            case "month":
                return new SimpleDateFormat("yyyy-MM");
            default:
                return new SimpleDateFormat("yyyy-MM-dd HH:00:00");
        }
    }

    /**
     * 计算高峰时段
     */
    private List<TimeframeResponse.PeakHourDTO> calculatePeakHours(List<TimeframeDataDTO> data) {
        // 按小时分组统计
        Map<Integer, List<TimeframeDataDTO>> hourlyData = new HashMap<>();
        for (TimeframeDataDTO d : data) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(d.getTimestamp());
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            hourlyData.computeIfAbsent(hour, k -> new ArrayList<>()).add(d);
        }
        
        // 计算每小时的平均事件数和分数
        List<TimeframeResponse.PeakHourDTO> peakHours = new ArrayList<>();
        for (Map.Entry<Integer, List<TimeframeDataDTO>> entry : hourlyData.entrySet()) {
            List<TimeframeDataDTO> hourData = entry.getValue();
            long totalEvents = hourData.stream().mapToLong(TimeframeDataDTO::getEventCount).sum();
            OptionalDouble avgScoreOpt = hourData.stream()
                .filter(d -> d.getAvgScore() != null)
                .mapToDouble(d -> d.getAvgScore().doubleValue())
                .average();
            BigDecimal avgScore = avgScoreOpt.isPresent() ? 
                BigDecimal.valueOf(avgScoreOpt.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
            
            TimeframeResponse.PeakHourDTO peakHour = new TimeframeResponse.PeakHourDTO();
            peakHour.setHour(entry.getKey());
            peakHour.setEventCount(totalEvents);
            peakHour.setAvgScore(avgScore);
            peakHours.add(peakHour);
        }
        
        // 按事件数排序，取前5个
        peakHours.sort((a, b) -> Long.compare(b.getEventCount(), a.getEventCount()));
        return peakHours.stream().limit(5).collect(Collectors.toList());
    }

    /**
     * 计算趋势（今日/昨日/本周/上周/本月/上月）
     */
    private EventStatisticsResponse.TrendDTO calculateTrend() {
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        
        // 今日开始时间
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long todayStart = cal.getTimeInMillis();
        
        // 昨日开始时间
        cal.add(Calendar.DAY_OF_MONTH, -1);
        long yesterdayStart = cal.getTimeInMillis();
        long yesterdayEnd = todayStart - 1;
        
        // 本周开始时间（周一）
        cal.setTimeInMillis(now);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysFromMonday = (dayOfWeek == Calendar.SUNDAY ? 6 : dayOfWeek - Calendar.MONDAY);
        cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long thisWeekStart = cal.getTimeInMillis();
        
        // 上周开始时间
        cal.add(Calendar.DAY_OF_MONTH, -7);
        long lastWeekStart = cal.getTimeInMillis();
        long lastWeekEnd = thisWeekStart - 1;
        
        // 本月开始时间
        cal.setTimeInMillis(now);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long thisMonthStart = cal.getTimeInMillis();
        
        // 上月开始时间
        cal.add(Calendar.MONTH, -1);
        long lastMonthStart = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, 1);
        long lastMonthEnd = thisMonthStart - 1;
        
        EventStatisticsResponse.TrendDTO trend = new EventStatisticsResponse.TrendDTO();
        trend.setToday((long) eventDataRepository.findByTimestampBetween(todayStart, now).size());
        trend.setYesterday((long) eventDataRepository.findByTimestampBetween(yesterdayStart, yesterdayEnd).size());
        trend.setThisWeek((long) eventDataRepository.findByTimestampBetween(thisWeekStart, now).size());
        trend.setLastWeek((long) eventDataRepository.findByTimestampBetween(lastWeekStart, lastWeekEnd).size());
        trend.setThisMonth((long) eventDataRepository.findByTimestampBetween(thisMonthStart, now).size());
        trend.setLastMonth((long) eventDataRepository.findByTimestampBetween(lastMonthStart, lastMonthEnd).size());
        
        return trend;
    }

    /**
     * 获取区域分析
     */
    public RegionAnalysisResponse getRegionAnalysis(Long startTime, Long endTime, String level) {
        List<EventDataV2> events = eventDataRepository.findByTimestampBetween(startTime, endTime);
        
        // 按区域分组（从location_region字段解析）
        Map<String, List<EventDataV2>> eventsByRegion = new HashMap<>();
        
        for (EventDataV2 event : events) {
            String region = extractRegion(event.getLocationRegion(), level);
            if (region != null && !region.isEmpty()) {
                eventsByRegion.computeIfAbsent(region, k -> new ArrayList<>()).add(event);
            }
        }
        
        List<RegionAnalysisResponse.RegionAnalysisDTO> regions = new ArrayList<>();
        
        for (Map.Entry<String, List<EventDataV2>> entry : eventsByRegion.entrySet()) {
            List<EventDataV2> regionEvents = entry.getValue();
            
            // 计算区域中心点和边界
            BigDecimal centerLat = null;
            BigDecimal centerLng = null;
            double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
            double minLng = Double.MAX_VALUE, maxLng = -Double.MAX_VALUE;
            
            for (EventDataV2 event : regionEvents) {
                if (event.getLocationLat() != null && event.getLocationLng() != null) {
                    double lat = event.getLocationLat().doubleValue();
                    double lng = event.getLocationLng().doubleValue();
                    
                    minLat = Math.min(minLat, lat);
                    maxLat = Math.max(maxLat, lat);
                    minLng = Math.min(minLng, lng);
                    maxLng = Math.max(maxLng, lng);
                }
            }
            
            if (minLat != Double.MAX_VALUE) {
                centerLat = BigDecimal.valueOf((minLat + maxLat) / 2).setScale(6, RoundingMode.HALF_UP);
                centerLng = BigDecimal.valueOf((minLng + maxLng) / 2).setScale(6, RoundingMode.HALF_UP);
            }
            
            // 统计信息
            int eventCount = regionEvents.size();
            int criticalCount = (int) regionEvents.stream().filter(e -> "Level 3".equals(e.getLevel())).count();
            int highCount = (int) regionEvents.stream().filter(e -> "Level 2".equals(e.getLevel())).count();
            int mediumCount = (int) regionEvents.stream().filter(e -> "Level 1".equals(e.getLevel())).count();
            int lowCount = (int) regionEvents.stream().filter(e -> "Normal".equals(e.getLevel())).count();
            
            OptionalDouble avgScoreOpt = regionEvents.stream()
                .filter(e -> e.getScore() != null)
                .mapToDouble(e -> e.getScore().doubleValue())
                .average();
            BigDecimal avgScore = avgScoreOpt.isPresent() ? 
                BigDecimal.valueOf(avgScoreOpt.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
            
            // 计算风险级别
            String riskLevel = "low";
            if (criticalCount > 0 || highCount > eventCount * 0.3) {
                riskLevel = "high";
            } else if (highCount > 0 || mediumCount > eventCount * 0.3) {
                riskLevel = "medium";
            }
            
            // 构建热力图数据（简化：使用区域内的坐标点）
            List<RegionAnalysisResponse.HeatmapPointDTO> heatmap = new ArrayList<>();
            Map<String, List<EventDataV2>> gridMap = new HashMap<>();
            double gridSize = 0.005; // 约500米
            
            for (EventDataV2 event : regionEvents) {
                if (event.getLocationLat() != null && event.getLocationLng() != null) {
                    double lat = event.getLocationLat().doubleValue();
                    double lng = event.getLocationLng().doubleValue();
                    int gridLat = (int) (lat / gridSize);
                    int gridLng = (int) (lng / gridSize);
                    String gridKey = gridLat + "_" + gridLng;
                    gridMap.computeIfAbsent(gridKey, k -> new ArrayList<>()).add(event);
                }
            }
            
            int maxGridCount = gridMap.values().stream().mapToInt(List::size).max().orElse(1);
            for (Map.Entry<String, List<EventDataV2>> gridEntry : gridMap.entrySet()) {
                List<EventDataV2> gridEvents = gridEntry.getValue();
                OptionalDouble avgLatOpt = gridEvents.stream()
                    .filter(e -> e.getLocationLat() != null)
                    .mapToDouble(e -> e.getLocationLat().doubleValue())
                    .average();
                OptionalDouble avgLngOpt = gridEvents.stream()
                    .filter(e -> e.getLocationLng() != null)
                    .mapToDouble(e -> e.getLocationLng().doubleValue())
                    .average();
                
                if (avgLatOpt.isPresent() && avgLngOpt.isPresent()) {
                    RegionAnalysisResponse.HeatmapPointDTO heatmapPoint = new RegionAnalysisResponse.HeatmapPointDTO();
                    heatmapPoint.setLat(BigDecimal.valueOf(avgLatOpt.getAsDouble()).setScale(6, RoundingMode.HALF_UP));
                    heatmapPoint.setLng(BigDecimal.valueOf(avgLngOpt.getAsDouble()).setScale(6, RoundingMode.HALF_UP));
                    heatmapPoint.setIntensity(gridEvents.size() / (double) maxGridCount);
                    heatmapPoint.setEventCount(gridEvents.size());
                    heatmap.add(heatmapPoint);
                }
            }
            
            // 构建边界
            RegionAnalysisResponse.MapBoundsDTO bounds = null;
            if (minLat != Double.MAX_VALUE) {
                double latRange = maxLat - minLat;
                double lngRange = maxLng - minLng;
                bounds = new RegionAnalysisResponse.MapBoundsDTO(
                    maxLat + latRange * 0.1,
                    minLat - latRange * 0.1,
                    maxLng + lngRange * 0.1,
                    minLng - lngRange * 0.1
                );
            }
            
            RegionAnalysisResponse.RegionLocationDTO location = new RegionAnalysisResponse.RegionLocationDTO();
            location.setCenterLat(centerLat);
            location.setCenterLng(centerLng);
            location.setBounds(bounds);
            
            RegionAnalysisResponse.RegionStatisticsDTO statistics = new RegionAnalysisResponse.RegionStatisticsDTO();
            statistics.setEventCount(eventCount);
            statistics.setCriticalCount(criticalCount);
            statistics.setHighCount(highCount);
            statistics.setMediumCount(mediumCount);
            statistics.setLowCount(lowCount);
            statistics.setAvgScore(avgScore);
            statistics.setRiskLevel(riskLevel);
            
            RegionAnalysisResponse.RegionAnalysisDTO region = new RegionAnalysisResponse.RegionAnalysisDTO();
            region.setRegionId("REGION_" + entry.getKey().hashCode());
            region.setRegionName(entry.getKey());
            region.setRegionType(level);
            region.setLocation(location);
            region.setStatistics(statistics);
            region.setHeatmap(heatmap);
            
            regions.add(region);
        }
        
        // 按事件数排序
        regions.sort((a, b) -> Integer.compare(b.getStatistics().getEventCount(), a.getStatistics().getEventCount()));
        
        // 构建高风险区域排行
        List<RegionAnalysisResponse.TopRegionDTO> topRegions = regions.stream()
            .limit(10)
            .map(r -> {
                RegionAnalysisResponse.TopRegionDTO topRegion = new RegionAnalysisResponse.TopRegionDTO();
                topRegion.setRegionName(r.getRegionName());
                topRegion.setEventCount(r.getStatistics().getEventCount());
                topRegion.setRiskLevel(r.getStatistics().getRiskLevel());
                return topRegion;
            })
            .collect(Collectors.toList());
        
        RegionAnalysisResponse response = new RegionAnalysisResponse();
        response.setLevel(level);
        response.setRegions(regions);
        response.setTopRegions(topRegions);
        
        return response;
    }

    /**
     * 从区域字符串提取指定级别的区域
     */
    private String extractRegion(String locationRegion, String level) {
        if (locationRegion == null || locationRegion.isEmpty()) {
            return null;
        }
        
        String[] parts = locationRegion.split("\\s+");
        
        switch (level) {
            case "province":
                return parts.length > 0 ? parts[0] : null;
            case "city":
                return parts.length > 1 ? parts[0] + " " + parts[1] : (parts.length > 0 ? parts[0] : null);
            case "district":
                return locationRegion; // 返回完整区域
            default:
                return locationRegion;
        }
    }
}

