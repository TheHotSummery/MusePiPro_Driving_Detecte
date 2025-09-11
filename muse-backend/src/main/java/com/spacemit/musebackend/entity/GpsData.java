package com.spacemit.musebackend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "gps_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GpsData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT")
    private Integer id;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    @Column(name = "`timestamp`", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "`utc_time`", length = 20)
    private String utcTime;

    @Column(name = "`utc_date`", length = 10)
    private String utcDate;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "hdop", precision = 4, scale = 2)
    private BigDecimal hdop;

    @Column(name = "altitude", precision = 8, scale = 2)
    private BigDecimal altitude;

    @Column(name = "fix_mode")
    private Integer fixMode;

    @Column(name = "course_over_ground", precision = 6, scale = 2)
    private BigDecimal courseOverGround;

    @Column(name = "speed_kmh", precision = 6, scale = 2)
    private BigDecimal speedKmh;

    @Column(name = "speed_knots", precision = 6, scale = 2)
    private BigDecimal speedKnots;

    @Column(name = "satellites")
    private Integer satellites;

    @Column(name = "raw_gps_data", columnDefinition = "TEXT")
    private String rawGpsData;

    // 疲劳驾驶相关字段
    @Column(name = "fatigue_score", precision = 4, scale = 3)
    private BigDecimal fatigueScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "fatigue_level")
    private FatigueLevel fatigueLevel;

    @Column(name = "eye_blink_rate", precision = 4, scale = 2)
    private BigDecimal eyeBlinkRate;

    @Column(name = "head_movement_score", precision = 4, scale = 2)
    private BigDecimal headMovementScore;

    @Column(name = "yawn_count")
    private Integer yawnCount;

    @Column(name = "attention_score", precision = 4, scale = 2)
    private BigDecimal attentionScore;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum FatigueLevel {
        NORMAL, MILD, MODERATE, SEVERE
    }
}
