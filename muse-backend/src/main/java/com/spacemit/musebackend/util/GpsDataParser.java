package com.spacemit.musebackend.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

@Component
@Slf4j
public class GpsDataParser {

    private static final Pattern GPS_DATA_PATTERN = Pattern.compile(
        "^([0-9]{6}\\.[0-9]{2}),([0-9]{4}\\.[0-9]{4}[NS]),([0-9]{5}\\.[0-9]{4}[EW]),([0-9]+\\.[0-9]+),([0-9]+\\.[0-9]+),([0-9]+),([0-9]+\\.[0-9]+|),([0-9]+\\.[0-9]+),([0-9]+\\.[0-9]+),([0-9]{6}),([0-9]+)$"
    );

    @Data
    public static class ParsedGpsData {
        private LocalDateTime timestamp;
        private String utcTime;
        private String utcDate;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private BigDecimal hdop;
        private BigDecimal altitude;
        private Integer fixMode;
        private BigDecimal courseOverGround;
        private BigDecimal speedKmh;
        private BigDecimal speedKnots;
        private Integer satellites;
    }

    /**
     * 解析GPS原始数据
     * 格式: 131750.00,3333.2240N,11901.8572E,1.23,32.4,3,,0.179,0.098,030925,11
     */
    public ParsedGpsData parseGpsData(String rawGpsData) {
        try {
            if (rawGpsData == null || rawGpsData.trim().isEmpty()) {
                throw new IllegalArgumentException("GPS数据为空");
            }

            String[] parts = rawGpsData.split(",");
            if (parts.length != 11) {
                throw new IllegalArgumentException("GPS数据格式错误，期望11个字段，实际" + parts.length + "个");
            }

            ParsedGpsData parsed = new ParsedGpsData();

            // 1. UTC时间 (131750.00)
            parsed.setUtcTime(parts[0]);
            parsed.setTimestamp(parseTimestamp(parts[0], parts[9])); // 使用时间和日期

            // 2. 纬度 (3333.2240N)
            parsed.setLatitude(parseLatitude(parts[1]));

            // 3. 经度 (11901.8572E)
            parsed.setLongitude(parseLongitude(parts[2]));

            // 4. HDOP (1.23)
            parsed.setHdop(new BigDecimal(parts[3]));

            // 5. 海拔高度 (32.4)
            parsed.setAltitude(new BigDecimal(parts[4]));

            // 6. 定位模式 (3)
            parsed.setFixMode(Integer.parseInt(parts[5]));

            // 7. 地面航向 (可能为空)
            if (!parts[6].isEmpty()) {
                parsed.setCourseOverGround(new BigDecimal(parts[6]));
            }

            // 8. 地面速率 km/h (0.179)
            parsed.setSpeedKmh(new BigDecimal(parts[7]));

            // 9. 地面速率 knots (0.098)
            parsed.setSpeedKnots(new BigDecimal(parts[8]));

            // 10. UTC日期 (030925)
            parsed.setUtcDate(parts[9]);

            // 11. 卫星数 (11)
            parsed.setSatellites(Integer.parseInt(parts[10]));

            log.debug("GPS数据解析成功: 纬度={}, 经度={}, 速度={}km/h, 卫星数={}", 
                     parsed.getLatitude(), parsed.getLongitude(), parsed.getSpeedKmh(), parsed.getSatellites());

            return parsed;

        } catch (Exception e) {
            log.error("GPS数据解析失败: {}", e.getMessage());
            throw new IllegalArgumentException("GPS数据解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析时间戳
     */
    private LocalDateTime parseTimestamp(String utcTime, String utcDate) {
        try {
            // 解析时间 HHMMSS.SS
            String timePart = utcTime.substring(0, 6); // 131750
            int hour = Integer.parseInt(timePart.substring(0, 2));
            int minute = Integer.parseInt(timePart.substring(2, 4));
            int second = Integer.parseInt(timePart.substring(4, 6));

            // 解析日期 DDMMYY
            int day = Integer.parseInt(utcDate.substring(0, 2));
            int month = Integer.parseInt(utcDate.substring(2, 4));
            int year = 2000 + Integer.parseInt(utcDate.substring(4, 6)); // 假设是20xx年

            return LocalDateTime.of(year, month, day, hour, minute, second);
        } catch (Exception e) {
            log.warn("时间解析失败，使用当前时间: {}", e.getMessage());
            return LocalDateTime.now();
        }
    }

    /**
     * 解析纬度
     * 格式: 3333.2240N -> 33.553733
     */
    private BigDecimal parseLatitude(String latStr) {
        try {
            String direction = latStr.substring(latStr.length() - 1);
            String coordStr = latStr.substring(0, latStr.length() - 1);

            // 解析度分格式 DDMM.MMMM
            int degrees = Integer.parseInt(coordStr.substring(0, 2));
            double minutes = Double.parseDouble(coordStr.substring(2));

            double decimalDegrees = degrees + (minutes / 60.0);
            if ("S".equals(direction)) {
                decimalDegrees = -decimalDegrees;
            }

            return new BigDecimal(decimalDegrees);
        } catch (Exception e) {
            throw new IllegalArgumentException("纬度解析失败: " + latStr);
        }
    }

    /**
     * 解析经度
     * 格式: 11901.8572E -> 119.030953
     */
    private BigDecimal parseLongitude(String lngStr) {
        try {
            String direction = lngStr.substring(lngStr.length() - 1);
            String coordStr = lngStr.substring(0, lngStr.length() - 1);

            // 解析度分格式 DDDMM.MMMM
            int degrees = Integer.parseInt(coordStr.substring(0, 3));
            double minutes = Double.parseDouble(coordStr.substring(3));

            double decimalDegrees = degrees + (minutes / 60.0);
            if ("W".equals(direction)) {
                decimalDegrees = -decimalDegrees;
            }

            return new BigDecimal(decimalDegrees);
        } catch (Exception e) {
            throw new IllegalArgumentException("经度解析失败: " + lngStr);
        }
    }

    /**
     * 验证GPS数据质量
     */
    public boolean isGpsDataValid(ParsedGpsData data) {
        if (data == null) return false;
        
        // 检查基本字段
        if (data.getLatitude() == null || data.getLongitude() == null) return false;
        if (data.getSatellites() == null || data.getSatellites() < 3) return false;
        if (data.getFixMode() == null || data.getFixMode() < 2) return false;
        
        // 检查坐标范围
        if (data.getLatitude().doubleValue() < -90 || data.getLatitude().doubleValue() > 90) return false;
        if (data.getLongitude().doubleValue() < -180 || data.getLongitude().doubleValue() > 180) return false;
        
        // 检查HDOP（水平精度因子）
        if (data.getHdop() != null && data.getHdop().doubleValue() > 10) return false;
        
        return true;
    }

    /**
     * 获取GPS质量评级
     */
    public String getGpsQualityRating(ParsedGpsData data) {
        if (!isGpsDataValid(data)) return "POOR";
        
        int satellites = data.getSatellites();
        double hdop = data.getHdop() != null ? data.getHdop().doubleValue() : 10.0;
        
        if (satellites >= 10 && hdop <= 1.0) return "EXCELLENT";
        if (satellites >= 8 && hdop <= 2.0) return "GOOD";
        if (satellites >= 6 && hdop <= 5.0) return "FAIR";
        return "POOR";
    }
}
