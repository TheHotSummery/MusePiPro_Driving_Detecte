package com.spacemit.musebackendv2.entity.v2;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * V2版本事件数据实体类
 * 对应硬件上报的 dataType: "event"
 */
@Entity
@Table(name = "event_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventDataV2 implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "event_id", unique = true, nullable = false, length = 100)
    private String eventId;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    @Column(name = "driver_id", length = 50)
    private String driverId;

    @Column(name = "timestamp", nullable = false)
    private Long timestamp;

    @Column(name = "level", nullable = false, length = 20)
    private String level;

    @Column(name = "score", nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "behavior", nullable = false, length = 50)
    private String behavior;

    @Column(name = "event_type", length = 20)
    private String eventType;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "confidence", precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "duration", precision = 8, scale = 2)
    private BigDecimal duration;

    @Column(name = "location_lat", precision = 10, scale = 6)
    private BigDecimal locationLat;

    @Column(name = "location_lng", precision = 11, scale = 6)
    private BigDecimal locationLng;

    @Column(name = "location_address", length = 255)
    private String locationAddress;

    @Column(name = "location_region", length = 100)
    private String locationRegion;

    @Column(name = "distracted_count")
    private Integer distractedCount = 0;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = System.currentTimeMillis();
        }
    }
}
















