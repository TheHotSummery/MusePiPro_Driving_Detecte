package com.spacemit.musebackend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "realtime_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT")
    private Integer id;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "location_lat", precision = 10, scale = 8)
    private BigDecimal locationLat;

    @Column(name = "location_lng", precision = 11, scale = 8)
    private BigDecimal locationLng;

    @Column(name = "speed", precision = 6, scale = 2)
    private BigDecimal speed;

    @Column(name = "direction", precision = 6, scale = 2)
    private BigDecimal direction;

    @Column(name = "altitude", precision = 8, scale = 2)
    private BigDecimal altitude;

    @Column(name = "hdop", precision = 4, scale = 2)
    private BigDecimal hdop;

    @Column(name = "satellites")
    private Integer satellites;

    @Column(name = "fix_mode")
    private Integer fixMode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}