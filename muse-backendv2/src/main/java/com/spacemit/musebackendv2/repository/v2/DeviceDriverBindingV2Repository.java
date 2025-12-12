package com.spacemit.musebackendv2.repository.v2;

import com.spacemit.musebackendv2.entity.v2.DeviceDriverBindingV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * V2版本设备-驾驶员绑定Repository
 */
@Repository
public interface DeviceDriverBindingV2Repository extends JpaRepository<DeviceDriverBindingV2, Long> {
    
    Optional<DeviceDriverBindingV2> findByDeviceIdAndStatus(String deviceId, DeviceDriverBindingV2.BindingStatus status);
    
    List<DeviceDriverBindingV2> findByDriverIdAndStatus(String driverId, DeviceDriverBindingV2.BindingStatus status);
    
    @Query("SELECT b FROM DeviceDriverBindingV2 b WHERE b.deviceId = :deviceId AND b.status = 'ACTIVE' AND (b.unbindTime IS NULL OR b.unbindTime = 0)")
    Optional<DeviceDriverBindingV2> findActiveBindingByDeviceId(@Param("deviceId") String deviceId);
    
    @Query("SELECT b FROM DeviceDriverBindingV2 b WHERE b.driverId = :driverId AND b.status = 'ACTIVE' AND (b.unbindTime IS NULL OR b.unbindTime = 0)")
    List<DeviceDriverBindingV2> findActiveBindingsByDriverId(@Param("driverId") String driverId);
    
    List<DeviceDriverBindingV2> findByDriverId(String driverId);
}

