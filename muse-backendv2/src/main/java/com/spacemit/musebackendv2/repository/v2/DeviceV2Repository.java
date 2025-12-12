package com.spacemit.musebackendv2.repository.v2;

import com.spacemit.musebackendv2.entity.v2.DeviceV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * V2版本设备Repository
 */
@Repository
public interface DeviceV2Repository extends JpaRepository<DeviceV2, Long> {
    
    Optional<DeviceV2> findByDeviceId(String deviceId);
    
    Optional<DeviceV2> findByDeviceIdAndDeletedAtIsNull(String deviceId);
    
    List<DeviceV2> findByStatus(DeviceV2.DeviceStatus status);
    
    @Query("SELECT d FROM DeviceV2 d WHERE d.deletedAt IS NULL")
    List<DeviceV2> findAllActive();
}
















