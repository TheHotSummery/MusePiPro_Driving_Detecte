package com.spacemit.musebackendv2.service.dashboard;

import com.spacemit.musebackendv2.dto.dashboard.*;
import com.spacemit.musebackendv2.entity.v2.*;
import com.spacemit.musebackendv2.repository.v2.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 图表数据服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardChartsService {

    private final EventDataV2Repository eventDataRepository;
    private final StatusDataV2Repository statusDataRepository;
    private final DeviceV2Repository deviceRepository;
    private final DriverV2Repository driverRepository;
    private final DeviceDriverBindingV2Repository bindingRepository;

    /**
     * 获取疲劳趋势曲线
     */
    public TrendChartResponse getTrendChart(String deviceId, Long startTime, Long endTime, String interval) {
        // 获取状态数据（用于趋势曲线）
        List<StatusDataV2> statusList;
        if (deviceId != null && !deviceId.isEmpty()) {
            statusList = statusDataRepository.findByDeviceIdAndTimestampBetween(deviceId, startTime, endTime);
        } else {
            // 如果没有deviceId，从所有设备获取
            statusList = statusDataRepository.findByTimestampBetween(startTime, endTime);
            // 如果没有deviceId，需要从所有设备获取，这里简化处理
            statusList = new ArrayList<>();
        }
        
        // 获取事件数据
        List<EventDataV2> events;
        if (deviceId != null && !deviceId.isEmpty()) {
            events = eventDataRepository.findByDeviceIdAndTimestampBetween(deviceId, startTime, endTime);
        } else {
            events = eventDataRepository.findByTimestampBetween(startTime, endTime);
        }
        
        // 获取驾驶员信息
        String driverId = null;
        String driverName = null;
        if (deviceId != null && !deviceId.isEmpty()) {
            Optional<DeviceDriverBindingV2> bindingOpt = bindingRepository.findActiveBindingByDeviceId(deviceId);
            if (bindingOpt.isPresent()) {
                driverId = bindingOpt.get().getDriverId();
                Optional<DriverV2> driverOpt = driverRepository.findByDriverId(driverId);
                if (driverOpt.isPresent()) {
                    driverName = driverOpt.get().getDriverName();
                }
            }
        }
        
        // 按时间间隔分组
        Map<String, List<StatusDataV2>> statusByTime = groupStatusByInterval(statusList, interval);
        Map<String, List<EventDataV2>> eventsByTime = groupEventsByInterval(events, interval);
        
        // 构建时间序列（合并所有时间点）
        Set<String> allTimeKeys = new TreeSet<>();
        allTimeKeys.addAll(statusByTime.keySet());
        allTimeKeys.addAll(eventsByTime.keySet());
        
        // 构建疲劳分数序列
        List<TrendChartResponse.ChartDataPointDTO> scoreData = new ArrayList<>();
        List<TrendChartResponse.ChartDataPointDTO> levelData = new ArrayList<>();
        
        SimpleDateFormat sdf = getDateFormat(interval);
        Calendar cal = Calendar.getInstance();
        
        for (String timeKey : allTimeKeys) {
            List<StatusDataV2> timeStatus = statusByTime.getOrDefault(timeKey, Collections.emptyList());
            List<EventDataV2> timeEvents = eventsByTime.getOrDefault(timeKey, Collections.emptyList());
            
            // 计算平均分数
            Double avgScore = 0.0;
            String level = "Normal";
            if (!timeStatus.isEmpty()) {
                OptionalDouble avg = timeStatus.stream()
                    .filter(s -> s.getScore() != null)
                    .mapToDouble(s -> s.getScore().doubleValue())
                    .average();
                if (avg.isPresent()) {
                    avgScore = avg.getAsDouble();
                    // 根据分数确定级别
                    if (avgScore >= 85) {
                        level = "Level 3";
                    } else if (avgScore >= 70) {
                        level = "Level 2";
                    } else if (avgScore >= 60) {
                        level = "Level 1";
                    } else {
                        level = "Normal";
                    }
                }
            } else if (!timeEvents.isEmpty()) {
                // 如果没有状态数据，使用事件数据
                OptionalDouble avg = timeEvents.stream()
                    .filter(e -> e.getScore() != null)
                    .mapToDouble(e -> e.getScore().doubleValue())
                    .average();
                if (avg.isPresent()) {
                    avgScore = avg.getAsDouble();
                    level = timeEvents.get(0).getLevel();
                }
            }
            
            // 解析时间戳
            Long timestamp = parseTimeKey(timeKey, interval);
            
            TrendChartResponse.ChartDataPointDTO scorePoint = new TrendChartResponse.ChartDataPointDTO();
            scorePoint.setTime(timeKey);
            scorePoint.setTimestamp(timestamp);
            scorePoint.setValue(avgScore);
            scorePoint.setLevel(level);
            scoreData.add(scorePoint);
            
            // 告警级别序列（转换为数值：Normal=0, Level1=1, Level2=2, Level3=3）
            int levelValue = 0;
            if ("Level 3".equals(level)) {
                levelValue = 3;
            } else if ("Level 2".equals(level)) {
                levelValue = 2;
            } else if ("Level 1".equals(level)) {
                levelValue = 1;
            }
            
            TrendChartResponse.ChartDataPointDTO levelPoint = new TrendChartResponse.ChartDataPointDTO();
            levelPoint.setTime(timeKey);
            levelPoint.setTimestamp(timestamp);
            levelPoint.setValue((double) levelValue);
            levelPoint.setLevel(level);
            levelData.add(levelPoint);
        }
        
        // 构建序列
        List<TrendChartResponse.ChartSeriesDTO> series = new ArrayList<>();
        
        TrendChartResponse.ChartSeriesDTO scoreSeries = new TrendChartResponse.ChartSeriesDTO();
        scoreSeries.setName("疲劳分数");
        scoreSeries.setType("line");
        scoreSeries.setData(scoreData);
        series.add(scoreSeries);
        
        TrendChartResponse.ChartSeriesDTO levelSeries = new TrendChartResponse.ChartSeriesDTO();
        levelSeries.setName("告警级别");
        levelSeries.setType("bar");
        levelSeries.setData(levelData);
        series.add(levelSeries);
        
        // 统计信息
        TrendChartResponse.TrendStatisticsDTO statistics = new TrendChartResponse.TrendStatisticsDTO();
        OptionalDouble minScoreOpt = scoreData.stream()
            .filter(d -> d.getValue() != null && d.getValue() > 0)
            .mapToDouble(TrendChartResponse.ChartDataPointDTO::getValue)
            .min();
        OptionalDouble maxScoreOpt = scoreData.stream()
            .filter(d -> d.getValue() != null)
            .mapToDouble(TrendChartResponse.ChartDataPointDTO::getValue)
            .max();
        OptionalDouble avgScoreOpt = scoreData.stream()
            .filter(d -> d.getValue() != null && d.getValue() > 0)
            .mapToDouble(TrendChartResponse.ChartDataPointDTO::getValue)
            .average();
        
        statistics.setMinScore(minScoreOpt.isPresent() ? minScoreOpt.getAsDouble() : 0.0);
        statistics.setMaxScore(maxScoreOpt.isPresent() ? maxScoreOpt.getAsDouble() : 0.0);
        statistics.setAvgScore(avgScoreOpt.isPresent() ? avgScoreOpt.getAsDouble() : 0.0);
        statistics.setTotalEvents(events.size());
        
        TrendChartResponse response = new TrendChartResponse();
        response.setDeviceId(deviceId);
        response.setDriverId(driverId);
        response.setDriverName(driverName);
        response.setInterval(interval);
        response.setSeries(series);
        response.setStatistics(statistics);
        
        return response;
    }

    /**
     * 获取时间段分布图
     */
    public TimeDistributionResponse getTimeDistribution(Long startTime, Long endTime, String groupBy) {
        List<EventDataV2> events = eventDataRepository.findByTimestampBetween(startTime, endTime);
        
        // 按时间分组
        Map<String, List<EventDataV2>> groupedEvents = groupEventsByInterval(events, groupBy);
        
        List<TimeDistributionResponse.DistributionDataDTO> data = new ArrayList<>();
        SimpleDateFormat sdf = getDateFormat(groupBy);
        
        for (Map.Entry<String, List<EventDataV2>> entry : groupedEvents.entrySet()) {
            List<EventDataV2> groupEvents = entry.getValue();
            
            int totalCount = groupEvents.size();
            int criticalCount = (int) groupEvents.stream().filter(e -> "Level 3".equals(e.getLevel())).count();
            int highCount = (int) groupEvents.stream().filter(e -> "Level 2".equals(e.getLevel())).count();
            int mediumCount = (int) groupEvents.stream().filter(e -> "Level 1".equals(e.getLevel())).count();
            int lowCount = (int) groupEvents.stream().filter(e -> "Normal".equals(e.getLevel())).count();
            
            OptionalDouble avgScoreOpt = groupEvents.stream()
                .filter(e -> e.getScore() != null)
                .mapToDouble(e -> e.getScore().doubleValue())
                .average();
            double avgScore = avgScoreOpt.isPresent() ? avgScoreOpt.getAsDouble() : 0.0;
            
            TimeDistributionResponse.DistributionDataDTO distData = new TimeDistributionResponse.DistributionDataDTO();
            distData.setLabel(entry.getKey());
            distData.setValue(totalCount);
            distData.setCriticalCount(criticalCount);
            distData.setHighCount(highCount);
            distData.setMediumCount(mediumCount);
            distData.setLowCount(lowCount);
            distData.setAvgScore(avgScore);
            
            data.add(distData);
        }
        
        // 按标签排序
        data.sort(Comparator.comparing(TimeDistributionResponse.DistributionDataDTO::getLabel));
        
        // 识别高峰时段（连续时间段事件数较多）
        List<TimeDistributionResponse.PeakPeriodDTO> peakPeriods = identifyPeakPeriods(data, groupBy);
        
        TimeDistributionResponse response = new TimeDistributionResponse();
        response.setGroupBy(groupBy);
        response.setData(data);
        response.setPeakPeriods(peakPeriods);
        
        return response;
    }

    /**
     * 获取行为类型分布
     */
    public BehaviorDistributionResponse getBehaviorDistribution(Long startTime, Long endTime) {
        List<EventDataV2> events = eventDataRepository.findByTimestampBetween(startTime, endTime);
        
        // 按行为类型分组
        Map<String, List<EventDataV2>> eventsByBehavior = events.stream()
            .filter(e -> e.getBehavior() != null)
            .collect(Collectors.groupingBy(EventDataV2::getBehavior));
        
        int totalEvents = events.size();
        
        List<BehaviorDistributionResponse.BehaviorDataDTO> data = new ArrayList<>();
        
        for (Map.Entry<String, List<EventDataV2>> entry : eventsByBehavior.entrySet()) {
            List<EventDataV2> behaviorEvents = entry.getValue();
            int count = behaviorEvents.size();
            
            int criticalCount = (int) behaviorEvents.stream().filter(e -> "Level 3".equals(e.getLevel())).count();
            int highCount = (int) behaviorEvents.stream().filter(e -> "Level 2".equals(e.getLevel())).count();
            int mediumCount = (int) behaviorEvents.stream().filter(e -> "Level 1".equals(e.getLevel())).count();
            int lowCount = (int) behaviorEvents.stream().filter(e -> "Normal".equals(e.getLevel())).count();
            
            OptionalDouble avgScoreOpt = behaviorEvents.stream()
                .filter(e -> e.getScore() != null)
                .mapToDouble(e -> e.getScore().doubleValue())
                .average();
            double avgScore = avgScoreOpt.isPresent() ? avgScoreOpt.getAsDouble() : 0.0;
            
            double percentage = totalEvents > 0 ? (count * 100.0 / totalEvents) : 0.0;
            
            BehaviorDistributionResponse.BehaviorDataDTO behaviorData = new BehaviorDistributionResponse.BehaviorDataDTO();
            behaviorData.setBehavior(entry.getKey());
            behaviorData.setBehaviorName(getBehaviorName(entry.getKey()));
            behaviorData.setCount(count);
            behaviorData.setCriticalCount(criticalCount);
            behaviorData.setHighCount(highCount);
            behaviorData.setMediumCount(mediumCount);
            behaviorData.setLowCount(lowCount);
            behaviorData.setAvgScore(avgScore);
            behaviorData.setPercentage(percentage);
            
            data.add(behaviorData);
        }
        
        // 按数量排序
        data.sort((a, b) -> Integer.compare(b.getCount(), a.getCount()));
        
        // 统计信息
        BehaviorDistributionResponse.BehaviorStatisticsDTO statistics = new BehaviorDistributionResponse.BehaviorStatisticsDTO();
        statistics.setTotalBehaviors(data.size());
        statistics.setTotalEvents(totalEvents);
        if (!data.isEmpty()) {
            statistics.setTopBehavior(data.get(0).getBehavior());
            statistics.setTopBehaviorPercentage(data.get(0).getPercentage());
        }
        
        BehaviorDistributionResponse response = new BehaviorDistributionResponse();
        response.setData(data);
        response.setStatistics(statistics);
        
        return response;
    }

    /**
     * 获取区域分布图
     */
    public RegionDistributionResponse getRegionDistribution(Long startTime, Long endTime, String level) {
        List<EventDataV2> events = eventDataRepository.findByTimestampBetween(startTime, endTime);
        
        // 按区域分组（从location_region字段解析）
        Map<String, List<EventDataV2>> eventsByRegion = new HashMap<>();
        
        for (EventDataV2 event : events) {
            String region = extractRegion(event.getLocationRegion(), level);
            if (region != null && !region.isEmpty()) {
                eventsByRegion.computeIfAbsent(region, k -> new ArrayList<>()).add(event);
            }
        }
        
        int totalEvents = events.size();
        
        List<RegionDistributionResponse.RegionDataDTO> data = new ArrayList<>();
        
        for (Map.Entry<String, List<EventDataV2>> entry : eventsByRegion.entrySet()) {
            List<EventDataV2> regionEvents = entry.getValue();
            int count = regionEvents.size();
            
            int criticalCount = (int) regionEvents.stream().filter(e -> "Level 3".equals(e.getLevel())).count();
            int highCount = (int) regionEvents.stream().filter(e -> "Level 2".equals(e.getLevel())).count();
            int mediumCount = (int) regionEvents.stream().filter(e -> "Level 1".equals(e.getLevel())).count();
            int lowCount = (int) regionEvents.stream().filter(e -> "Normal".equals(e.getLevel())).count();
            
            OptionalDouble avgScoreOpt = regionEvents.stream()
                .filter(e -> e.getScore() != null)
                .mapToDouble(e -> e.getScore().doubleValue())
                .average();
            double avgScore = avgScoreOpt.isPresent() ? avgScoreOpt.getAsDouble() : 0.0;
            
            double percentage = totalEvents > 0 ? (count * 100.0 / totalEvents) : 0.0;
            
            RegionDistributionResponse.RegionDataDTO regionData = new RegionDistributionResponse.RegionDataDTO();
            regionData.setRegionName(entry.getKey());
            regionData.setRegionType(level);
            regionData.setCount(count);
            regionData.setCriticalCount(criticalCount);
            regionData.setHighCount(highCount);
            regionData.setMediumCount(mediumCount);
            regionData.setLowCount(lowCount);
            regionData.setAvgScore(avgScore);
            regionData.setPercentage(percentage);
            
            data.add(regionData);
        }
        
        // 按数量排序
        data.sort((a, b) -> Integer.compare(b.getCount(), a.getCount()));
        
        // 构建高风险区域排行
        List<RegionDistributionResponse.TopRegionDTO> topRegions = data.stream()
            .limit(10)
            .map(d -> {
                String riskLevel = "low";
                if (d.getCriticalCount() > 0 || d.getHighCount() > d.getCount() * 0.3) {
                    riskLevel = "high";
                } else if (d.getHighCount() > 0 || d.getMediumCount() > d.getCount() * 0.3) {
                    riskLevel = "medium";
                }
                
                RegionDistributionResponse.TopRegionDTO topRegion = new RegionDistributionResponse.TopRegionDTO();
                topRegion.setRegionName(d.getRegionName());
                topRegion.setEventCount(d.getCount());
                topRegion.setRiskLevel(riskLevel);
                return topRegion;
            })
            .collect(Collectors.toList());
        
        RegionDistributionResponse response = new RegionDistributionResponse();
        response.setLevel(level);
        response.setData(data);
        response.setTopRegions(topRegions);
        
        return response;
    }

    // ========== 辅助方法 ==========
    
    private Map<String, List<StatusDataV2>> groupStatusByInterval(List<StatusDataV2> statusList, String interval) {
        Map<String, List<StatusDataV2>> grouped = new HashMap<>();
        SimpleDateFormat sdf = getDateFormat(interval);
        Calendar cal = Calendar.getInstance();
        
        for (StatusDataV2 status : statusList) {
            cal.setTimeInMillis(status.getTimestamp());
            String key = formatTimeKey(cal, interval, sdf);
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(status);
        }
        
        return grouped;
    }
    
    private Map<String, List<EventDataV2>> groupEventsByInterval(List<EventDataV2> events, String interval) {
        Map<String, List<EventDataV2>> grouped = new HashMap<>();
        SimpleDateFormat sdf = getDateFormat(interval);
        Calendar cal = Calendar.getInstance();
        
        for (EventDataV2 event : events) {
            cal.setTimeInMillis(event.getTimestamp());
            String key = formatTimeKey(cal, interval, sdf);
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(event);
        }
        
        return grouped;
    }
    
    private String formatTimeKey(Calendar cal, String interval, SimpleDateFormat sdf) {
        switch (interval) {
            case "minute":
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                return sdf.format(cal.getTime());
            case "hour":
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                return sdf.format(cal.getTime());
            case "day":
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                return sdf.format(cal.getTime());
            default:
                return sdf.format(cal.getTime());
        }
    }
    
    private SimpleDateFormat getDateFormat(String interval) {
        switch (interval) {
            case "minute":
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:00");
            case "hour":
                return new SimpleDateFormat("yyyy-MM-dd HH:00:00");
            case "day":
                return new SimpleDateFormat("yyyy-MM-dd");
            case "week":
                return new SimpleDateFormat("yyyy-MM-dd");
            case "month":
                return new SimpleDateFormat("yyyy-MM");
            default:
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:00");
        }
    }
    
    private Long parseTimeKey(String timeKey, String interval) {
        try {
            SimpleDateFormat sdf = getDateFormat(interval);
            return sdf.parse(timeKey).getTime();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }
    
    private List<TimeDistributionResponse.PeakPeriodDTO> identifyPeakPeriods(
            List<TimeDistributionResponse.DistributionDataDTO> data, String groupBy) {
        List<TimeDistributionResponse.PeakPeriodDTO> peakPeriods = new ArrayList<>();
        
        if (data.size() < 2) {
            return peakPeriods;
        }
        
        // 计算平均值
        double avgCount = data.stream().mapToInt(TimeDistributionResponse.DistributionDataDTO::getValue).average().orElse(0.0);
        
        // 找出连续的高峰时段
        int startIdx = -1;
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getValue() > avgCount * 1.5) {
                if (startIdx == -1) {
                    startIdx = i;
                }
            } else {
                if (startIdx != -1 && i - startIdx >= 2) {
                    // 找到高峰时段
                    int totalEvents = data.subList(startIdx, i).stream()
                        .mapToInt(TimeDistributionResponse.DistributionDataDTO::getValue)
                        .sum();
                    double avgScore = data.subList(startIdx, i).stream()
                        .mapToDouble(TimeDistributionResponse.DistributionDataDTO::getAvgScore)
                        .average()
                        .orElse(0.0);
                    
                    TimeDistributionResponse.PeakPeriodDTO peak = new TimeDistributionResponse.PeakPeriodDTO();
                    peak.setStart(startIdx);
                    peak.setEnd(i - 1);
                    peak.setLabel(data.get(startIdx).getLabel() + "-" + data.get(i - 1).getLabel());
                    peak.setEventCount(totalEvents);
                    peak.setAvgScore(avgScore);
                    peakPeriods.add(peak);
                }
                startIdx = -1;
            }
        }
        
        return peakPeriods;
    }
    
    private String getBehaviorName(String behavior) {
        Map<String, String> behaviorNames = new HashMap<>();
        behaviorNames.put("eyes_closed", "闭眼");
        behaviorNames.put("yarning", "打哈欠");
        behaviorNames.put("head_down", "低头");
        behaviorNames.put("seeing_left", "左看");
        behaviorNames.put("seeing_right", "右看");
        behaviorNames.put("eyes_closed_head_left", "闭眼左偏");
        behaviorNames.put("eyes_closed_head_right", "闭眼右偏");
        return behaviorNames.getOrDefault(behavior, behavior);
    }
    
    private String extractRegion(String locationRegion, String level) {
        if (locationRegion == null || locationRegion.isEmpty()) {
            return null;
        }
        
        // 解析区域字符串，格式：省 市 区
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

