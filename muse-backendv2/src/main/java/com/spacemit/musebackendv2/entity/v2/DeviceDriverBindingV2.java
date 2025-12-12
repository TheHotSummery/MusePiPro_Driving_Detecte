package com.spacemit.musebackendv2.entity.v2;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

/**
 * V2版本设备-驾驶员绑定实体类
 */
@Entity
@Table(name = "device_driver_bindings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDriverBindingV2 implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    @Column(name = "driver_id", nullable = false, length = 50)
    private String driverId;

    @Column(name = "bind_time", nullable = false)
    private Long bindTime;

    @Column(name = "unbind_time")
    private Long unbindTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private BindingStatus status = BindingStatus.ACTIVE;

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

    public enum BindingStatus {
        ACTIVE, INACTIVE
    }
}
















