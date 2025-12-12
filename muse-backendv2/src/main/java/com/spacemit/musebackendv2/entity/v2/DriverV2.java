package com.spacemit.musebackendv2.entity.v2;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * V2版本驾驶员实体类
 */
@Entity
@Table(name = "drivers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverV2 implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "driver_id", unique = true, nullable = false, length = 50)
    private String driverId;

    @Column(name = "driver_name", nullable = false, length = 100)
    private String driverName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    @Column(name = "license_type", length = 10)
    private String licenseType;

    @Column(name = "license_expire")
    private LocalDate licenseExpire;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "team_id", length = 50)
    private String teamId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private DriverStatus status = DriverStatus.ACTIVE;

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

    public enum DriverStatus {
        ACTIVE, INACTIVE
    }
}
















