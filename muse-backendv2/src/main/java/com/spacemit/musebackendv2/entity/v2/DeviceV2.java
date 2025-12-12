package com.spacemit.musebackendv2.entity.v2;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

/**
 * V2版本设备实体类
 */
@Entity
@Table(name = "devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceV2 implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "device_id", unique = true, nullable = false, length = 50)
    private String deviceId;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "version", length = 20)
    private String version;

    @Column(name = "first_report_time")
    private Long firstReportTime;

    @Column(name = "last_report_time")
    private Long lastReportTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private DeviceStatus status = DeviceStatus.OFFLINE;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @Column(name = "deleted_at")
    private Long deletedAt;

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

    public enum DeviceStatus {
        ONLINE, OFFLINE, ERROR
    }
}
















