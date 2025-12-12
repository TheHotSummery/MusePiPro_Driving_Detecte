package com.spacemit.musebackendv2.entity.v2;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * V2版本状态数据实体类
 * 对应硬件上报的 dataType: "status"
 */
@Entity
@Table(name = "status_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusDataV2 implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

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

    @Column(name = "location_lat", precision = 10, scale = 6)
    private BigDecimal locationLat;

    @Column(name = "location_lng", precision = 11, scale = 6)
    private BigDecimal locationLng;

    @Column(name = "cpu_usage", precision = 5, scale = 2)
    private BigDecimal cpuUsage;

    @Column(name = "memory_usage", precision = 5, scale = 2)
    private BigDecimal memoryUsage;

    @Column(name = "temperature", precision = 5, scale = 2)
    private BigDecimal temperature;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = System.currentTimeMillis();
        }
    }
}
















