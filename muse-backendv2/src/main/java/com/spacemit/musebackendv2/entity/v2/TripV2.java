package com.spacemit.musebackendv2.entity.v2;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * V2版本行程实体类
 */
@Entity
@Table(name = "trips")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripV2 implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "trip_id", unique = true, nullable = false, length = 50)
    private String tripId;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    @Column(name = "driver_id", length = 50)
    private String driverId;

    @Column(name = "start_time", nullable = false)
    private Long startTime;

    @Column(name = "end_time")
    private Long endTime;

    @Column(name = "start_lat", precision = 10, scale = 6)
    private BigDecimal startLat;

    @Column(name = "start_lng", precision = 11, scale = 6)
    private BigDecimal startLng;

    @Column(name = "start_address", length = 255)
    private String startAddress;

    @Column(name = "end_lat", precision = 10, scale = 6)
    private BigDecimal endLat;

    @Column(name = "end_lng", precision = 11, scale = 6)
    private BigDecimal endLng;

    @Column(name = "end_address", length = 255)
    private String endAddress;

    @Column(name = "total_distance", precision = 10, scale = 2)
    private BigDecimal totalDistance = BigDecimal.ZERO;

    @Column(name = "total_duration")
    private Integer totalDuration = 0;

    @Column(name = "event_count")
    private Integer eventCount = 0;

    @Column(name = "critical_event_count")
    private Integer criticalEventCount = 0;

    @Column(name = "high_event_count")
    private Integer highEventCount = 0;

    @Column(name = "medium_event_count")
    private Integer mediumEventCount = 0;

    @Column(name = "low_event_count")
    private Integer lowEventCount = 0;

    @Column(name = "max_level", length = 20)
    private String maxLevel;

    @Column(name = "max_score", precision = 5, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "avg_score", precision = 5, scale = 2)
    private BigDecimal avgScore;

    @Column(name = "safety_score", precision = 5, scale = 2)
    private BigDecimal safetyScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private TripStatus status = TripStatus.ONGOING;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @PrePersist
    protected void onCreate() {
        long now = System.currentTimeMillis();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = System.currentTimeMillis();
    }

    public enum TripStatus {
        ONGOING, COMPLETED, CANCELLED
    }
}
















