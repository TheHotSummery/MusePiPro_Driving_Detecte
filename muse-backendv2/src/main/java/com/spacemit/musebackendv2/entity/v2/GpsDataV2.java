package com.spacemit.musebackendv2.entity.v2;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * V2版本GPS数据实体类
 * 对应硬件上报的 dataType: "gps"
 */
@Entity
@Table(name = "gps_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GpsDataV2 implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    @Column(name = "driver_id", length = 50)
    private String driverId;

    @Column(name = "trip_id", length = 50)
    private String tripId;

    @Column(name = "timestamp", nullable = false)
    private Long timestamp;

    @Column(name = "location_lat", precision = 10, scale = 6)
    private BigDecimal locationLat;

    @Column(name = "location_lng", precision = 11, scale = 6)
    private BigDecimal locationLng;

    @Column(name = "speed", precision = 6, scale = 2)
    private BigDecimal speed;

    @Column(name = "direction", precision = 6, scale = 2)
    private BigDecimal direction;

    @Column(name = "altitude", precision = 8, scale = 2)
    private BigDecimal altitude;

    @Column(name = "satellites")
    private Integer satellites;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = System.currentTimeMillis();
        }
    }
}













import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * V2版本GPS数据实体类
 * 对应硬件上报的 dataType: "gps"
 */
@Entity
@Table(name = "gps_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GpsDataV2 implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    @Column(name = "driver_id", length = 50)
    private String driverId;

    @Column(name = "trip_id", length = 50)
    private String tripId;

    @Column(name = "timestamp", nullable = false)
    private Long timestamp;

    @Column(name = "location_lat", precision = 10, scale = 6)
    private BigDecimal locationLat;

    @Column(name = "location_lng", precision = 11, scale = 6)
    private BigDecimal locationLng;

    @Column(name = "speed", precision = 6, scale = 2)
    private BigDecimal speed;

    @Column(name = "direction", precision = 6, scale = 2)
    private BigDecimal direction;

    @Column(name = "altitude", precision = 8, scale = 2)
    private BigDecimal altitude;

    @Column(name = "satellites")
    private Integer satellites;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = System.currentTimeMillis();
        }
    }
}












