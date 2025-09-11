package com.spacemit.musebackend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT")
    private Integer id;

    @Column(name = "event_id", unique = true, nullable = false, length = 50)
    private String eventId;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity;

    @Column(name = "location_lat", precision = 10, scale = 8)
    private BigDecimal locationLat;

    @Column(name = "location_lng", precision = 11, scale = 8)
    private BigDecimal locationLng;

    @Column(name = "behavior", length = 50)
    private String behavior;

    @Column(name = "confidence", precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "duration", precision = 8, scale = 2)
    private BigDecimal duration;

    @Column(name = "alert_level", length = 20)
    private String alertLevel;

    @Column(name = "gpio_triggered", columnDefinition = "TEXT")
    private String gpioTriggered; // JSON字符串

    @Column(name = "context", columnDefinition = "TEXT")
    private String context; // JSON字符串

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum EventType {
        FATIGUE, DISTRACTION, EMERGENCY, SYSTEM
    }

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}