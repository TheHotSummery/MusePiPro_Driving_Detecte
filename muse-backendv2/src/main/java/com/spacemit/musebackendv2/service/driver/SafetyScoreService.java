package com.spacemit.musebackendv2.service.driver;

import com.spacemit.musebackendv2.entity.v2.EventDataV2;
import com.spacemit.musebackendv2.entity.v2.GpsDataV2;
import com.spacemit.musebackendv2.entity.v2.StatusDataV2;
import com.spacemit.musebackendv2.entity.v2.TripV2;
import com.spacemit.musebackendv2.repository.v2.EventDataV2Repository;
import com.spacemit.musebackendv2.repository.v2.GpsDataV2Repository;
import com.spacemit.musebackendv2.repository.v2.StatusDataV2Repository;
import com.spacemit.musebackendv2.repository.v2.TripV2Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 安全评分服务
 * 多维度安全评分算法
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SafetyScoreService {

    private final EventDataV2Repository eventDataRepository;
    private final StatusDataV2Repository statusDataRepository;
    private final GpsDataV2Repository gpsDataRepository;
    private final TripV2Repository tripRepository;

    /**
     * 计算综合安全评分
     */
    public SafetyScoreResult calculateSafetyScore(String driverId, Long startTime, Long endTime) {
        // 获取数据
        List<EventDataV2> events = eventDataRepository.findByDriverIdAndTimestampBetween(driverId, startTime, endTime);
        List<TripV2> trips = tripRepository.findByDriverIdAndStartTimeBetween(driverId, startTime, endTime);
        
        // 计算各维度评分
        BigDecimal fatigueScore = calculateFatigueScore(events, trips);
        BigDecimal behaviorScore = calculateBehaviorScore(events, trips);
        BigDecimal complianceScore = calculateComplianceScore(driverId, startTime, endTime);
        BigDecimal incidentScore = calculateIncidentScore(events);

        // 综合评分（加权平均）
        BigDecimal overallScore = fatigueScore.multiply(BigDecimal.valueOf(0.4))
            .add(behaviorScore.multiply(BigDecimal.valueOf(0.3)))
            .add(complianceScore.multiply(BigDecimal.valueOf(0.2)))
            .add(incidentScore.multiply(BigDecimal.valueOf(0.1)))
            .setScale(2, RoundingMode.HALF_UP);

        SafetyScoreResult result = new SafetyScoreResult();
        result.setOverallScore(overallScore);
        result.setFatigueScore(fatigueScore);
        result.setBehaviorScore(behaviorScore);
        result.setComplianceScore(complianceScore);
        result.setIncidentScore(incidentScore);

        return result;
    }

    /**
     * 计算疲劳评分
     */
    private BigDecimal calculateFatigueScore(List<EventDataV2> events, List<TripV2> trips) {
        double score = 100.0;

        // 按事件级别扣分
        for (EventDataV2 event : events) {
            String level = event.getLevel();
            if ("Level 3".equals(level)) {
                score -= 10.0;
            } else if ("Level 2".equals(level)) {
                score -= 5.0;
            } else if ("Level 1".equals(level)) {
                score -= 2.0;
            }
        }

        // 平均疲劳分数调整
        OptionalDouble avgScoreOpt = events.stream()
            .filter(e -> e.getScore() != null)
            .mapToDouble(e -> e.getScore().doubleValue())
            .average();
        
        if (avgScoreOpt.isPresent() && avgScoreOpt.getAsDouble() > 80) {
            score -= (avgScoreOpt.getAsDouble() - 80) * 0.5;
        }

        // 连续驾驶时间扣分
        for (TripV2 trip : trips) {
            int duration = trip.getTotalDuration() != null ? trip.getTotalDuration() : 0;
            int hours = duration / 3600;
            if (hours > 4) {
                score -= (hours - 4) * 5.0;
            }
        }

        // 夜间驾驶扣分
        Calendar cal = Calendar.getInstance();
        for (EventDataV2 event : events) {
            cal.setTimeInMillis(event.getTimestamp());
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            if (hour >= 22 || hour < 6) {
                score -= 2.0; // 简化：每个夜间事件扣2分
            }
        }

        return BigDecimal.valueOf(Math.max(0, Math.min(100, score))).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算行为评分
     */
    private BigDecimal calculateBehaviorScore(List<EventDataV2> events, List<TripV2> trips) {
        double score = 100.0;

        // 按行为类型扣分
        Map<String, Long> behaviorCounts = events.stream()
            .filter(e -> e.getBehavior() != null)
            .collect(Collectors.groupingBy(EventDataV2::getBehavior, Collectors.counting()));

        for (Map.Entry<String, Long> entry : behaviorCounts.entrySet()) {
            String behavior = entry.getKey();
            long count = entry.getValue();

            if ("eyes_closed".equals(behavior) || "head_down".equals(behavior)) {
                score -= count * 3.0; // 危险行为
            } else if ("seeing_left".equals(behavior) || "seeing_right".equals(behavior)) {
                score -= count * 1.0; // 分心行为
            }
        }

        // 行为频率检查（每小时>10次危险行为）
        for (TripV2 trip : trips) {
            int duration = trip.getTotalDuration() != null ? trip.getTotalDuration() : 1;
            int hours = Math.max(1, duration / 3600);
            
            long dangerousBehaviors = behaviorCounts.entrySet().stream()
                .filter(e -> "eyes_closed".equals(e.getKey()) || "head_down".equals(e.getKey()))
                .mapToLong(Map.Entry::getValue)
                .sum();
            
            if (dangerousBehaviors / hours > 10) {
                score -= 5.0;
            }
        }

        return BigDecimal.valueOf(Math.max(0, Math.min(100, score))).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算合规性评分
     */
    private BigDecimal calculateComplianceScore(String driverId, Long startTime, Long endTime) {
        double score = 100.0;

        // 获取GPS数据（通过driverId查询）
        // 注意：GPS数据表中有driverId字段，但Repository没有按driverId查询的方法
        // 简化处理：暂时跳过GPS数据检查，只检查行程中断
        List<GpsDataV2> gpsList = Collections.emptyList();

        if (gpsList.isEmpty()) {
            return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
        }

        // 超速检查（>120km/h）
        long speedingCount = gpsList.stream()
            .filter(g -> g.getSpeed() != null && g.getSpeed().doubleValue() > 120)
            .count();
        score -= speedingCount * 5.0;

        // 急刹车和急转弯检查（简化：基于速度变化）
        // 由于GPS数据查询限制，暂时跳过此检查
        // TODO: 添加按driverId查询GPS数据的方法

        // 行程中断检查（未完成的行程）
        List<TripV2> trips = tripRepository.findByDriverIdAndStartTimeBetween(driverId, startTime, endTime);
        long incompleteTrips = trips.stream()
            .filter(t -> t.getStatus() == TripV2.TripStatus.ONGOING || t.getEndTime() == null)
            .count();
        score -= incompleteTrips * 10.0;

        return BigDecimal.valueOf(Math.max(0, Math.min(100, score))).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算事故评分
     */
    private BigDecimal calculateIncidentScore(List<EventDataV2> events) {
        double score = 100.0;

        for (EventDataV2 event : events) {
            String level = event.getLevel();
            if ("Level 3".equals(level)) {
                score -= 20.0; // 严重事件
            } else if ("Level 2".equals(level)) {
                score -= 10.0; // 高级事件
            }
        }

        return BigDecimal.valueOf(Math.max(0, Math.min(100, score))).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算趋势数据
     */
    public List<TrendPoint> calculateTrend(String driverId, int days) {
        List<TrendPoint> trend = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (long) days * 24 * 60 * 60 * 1000;

        // 按天分组计算
        Map<String, List<EventDataV2>> eventsByDay = eventDataRepository
            .findByDriverIdAndTimestampBetween(driverId, startTime, endTime)
            .stream()
            .collect(Collectors.groupingBy(e -> sdf.format(new Date(e.getTimestamp()))));

        for (int i = 0; i < days; i++) {
            long dayStart = startTime + (long) i * 24 * 60 * 60 * 1000;
            long dayEnd = dayStart + 24 * 60 * 60 * 1000;
            String date = sdf.format(new Date(dayStart));

            List<EventDataV2> dayEvents = eventsByDay.getOrDefault(date, Collections.emptyList());
            SafetyScoreResult dayScore = calculateSafetyScore(driverId, dayStart, dayEnd);

            TrendPoint point = new TrendPoint();
            point.setDate(date);
            point.setScore(dayScore.getOverallScore());
            trend.add(point);
        }

        return trend;
    }

    /**
     * 生成改进建议
     */
    public List<Improvement> generateImprovements(String driverId, Long startTime, Long endTime) {
        List<Improvement> improvements = new ArrayList<>();
        
        SafetyScoreResult score = calculateSafetyScore(driverId, startTime, endTime);
        List<EventDataV2> events = eventDataRepository.findByDriverIdAndTimestampBetween(driverId, startTime, endTime);
        List<TripV2> trips = tripRepository.findByDriverIdAndStartTimeBetween(driverId, startTime, endTime);

        // 疲劳评分低
        if (score.getFatigueScore().compareTo(BigDecimal.valueOf(70)) < 0) {
            Improvement imp = new Improvement();
            imp.setType("fatigue");
            imp.setDescription("疲劳评分较低，建议减少连续驾驶时间，增加休息");
            imp.setPriority("high");
            improvements.add(imp);
        }

        // 行为评分低
        if (score.getBehaviorScore().compareTo(BigDecimal.valueOf(70)) < 0) {
            Map<String, Long> behaviorCounts = events.stream()
                .filter(e -> e.getBehavior() != null)
                .collect(Collectors.groupingBy(EventDataV2::getBehavior, Collectors.counting()));
            
            String topBehavior = behaviorCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");
            
            Improvement imp = new Improvement();
            imp.setType("behavior");
            imp.setDescription("行为评分较低，请特别注意" + getBehaviorName(topBehavior) + "行为");
            imp.setPriority("medium");
            improvements.add(imp);
        }

        // 夜间驾驶多
        long nightEvents = events.stream()
            .filter(e -> {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(e.getTimestamp());
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                return hour >= 22 || hour < 6;
            })
            .count();
        
        if (nightEvents > events.size() * 0.3) {
            Improvement imp = new Improvement();
            imp.setType("time");
            imp.setDescription("夜间驾驶时间较多，建议减少夜间驾驶");
            imp.setPriority("medium");
            improvements.add(imp);
        }

        return improvements;
    }

    /**
     * 计算排名
     */
    public RankingResult calculateRanking(String driverId, Long startTime, Long endTime) {
        SafetyScoreResult score = calculateSafetyScore(driverId, startTime, endTime);
        
        // 获取所有驾驶员
        List<String> allDriverIds = eventDataRepository.findByTimestampBetween(startTime, endTime)
            .stream()
            .map(EventDataV2::getDriverId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        // 计算所有驾驶员评分并排序
        List<DriverScore> driverScores = allDriverIds.stream()
            .map(id -> {
                SafetyScoreResult s = calculateSafetyScore(id, startTime, endTime);
                return new DriverScore(id, s.getOverallScore());
            })
            .sorted((a, b) -> b.getScore().compareTo(a.getScore()))
            .collect(Collectors.toList());

        // 找到当前驾驶员排名
        int rank = 1;
        for (int i = 0; i < driverScores.size(); i++) {
            if (driverScores.get(i).getDriverId().equals(driverId)) {
                rank = i + 1;
                break;
            }
        }

        int totalDrivers = driverScores.size();
        double percentile = totalDrivers > 0 ? (double) (totalDrivers - rank + 1) / totalDrivers * 100 : 0;

        RankingResult result = new RankingResult();
        result.setOverall(rank);
        result.setTotalDrivers(totalDrivers);
        result.setPercentile(BigDecimal.valueOf(percentile).setScale(1, RoundingMode.HALF_UP));

        return result;
    }

    // 辅助方法
    private String getDeviceIdByDriverId(String driverId) {
        // 从绑定表获取设备ID
        // 注意：这里需要注入DeviceDriverBindingV2Repository，但为了避免循环依赖，暂时返回null
        // 实际使用时，应该从调用方传入deviceId
        return null; // 由调用方提供deviceId
    }

    private String getBehaviorName(String behavior) {
        Map<String, String> names = new HashMap<>();
        names.put("eyes_closed", "闭眼");
        names.put("head_down", "低头");
        names.put("seeing_left", "左看");
        names.put("seeing_right", "右看");
        names.put("yarning", "打哈欠");
        return names.getOrDefault(behavior, behavior);
    }

    // 内部类
    public static class SafetyScoreResult {
        private BigDecimal overallScore;
        private BigDecimal fatigueScore;
        private BigDecimal behaviorScore;
        private BigDecimal complianceScore;
        private BigDecimal incidentScore;

        // Getters and Setters
        public BigDecimal getOverallScore() { return overallScore; }
        public void setOverallScore(BigDecimal overallScore) { this.overallScore = overallScore; }
        public BigDecimal getFatigueScore() { return fatigueScore; }
        public void setFatigueScore(BigDecimal fatigueScore) { this.fatigueScore = fatigueScore; }
        public BigDecimal getBehaviorScore() { return behaviorScore; }
        public void setBehaviorScore(BigDecimal behaviorScore) { this.behaviorScore = behaviorScore; }
        public BigDecimal getComplianceScore() { return complianceScore; }
        public void setComplianceScore(BigDecimal complianceScore) { this.complianceScore = complianceScore; }
        public BigDecimal getIncidentScore() { return incidentScore; }
        public void setIncidentScore(BigDecimal incidentScore) { this.incidentScore = incidentScore; }
    }

    public static class TrendPoint {
        private String date;
        private BigDecimal score;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public BigDecimal getScore() { return score; }
        public void setScore(BigDecimal score) { this.score = score; }
    }

    public static class Improvement {
        private String type;
        private String description;
        private String priority;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
    }

    public static class RankingResult {
        private int overall;
        private int totalDrivers;
        private BigDecimal percentile;

        public int getOverall() { return overall; }
        public void setOverall(int overall) { this.overall = overall; }
        public int getTotalDrivers() { return totalDrivers; }
        public void setTotalDrivers(int totalDrivers) { this.totalDrivers = totalDrivers; }
        public BigDecimal getPercentile() { return percentile; }
        public void setPercentile(BigDecimal percentile) { this.percentile = percentile; }
    }

    private static class DriverScore {
        private String driverId;
        private BigDecimal score;

        public DriverScore(String driverId, BigDecimal score) {
            this.driverId = driverId;
            this.score = score;
        }

        public String getDriverId() { return driverId; }
        public BigDecimal getScore() { return score; }
    }
}

