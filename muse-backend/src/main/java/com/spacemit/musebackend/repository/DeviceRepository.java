package com.spacemit.musebackend.repository;

import com.spacemit.musebackend.entity.Device;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Integer> {
    Optional<Device> findByDeviceId(String deviceId);
    boolean existsByDeviceId(String deviceId);
    
    List<Device> findByStatus(Device.DeviceStatus status);
    
    Page<Device> findByStatus(Device.DeviceStatus status, Pageable pageable);
    
    Page<Device> findByDeviceType(String deviceType, Pageable pageable);
    
    Page<Device> findByStatusAndDeviceType(Device.DeviceStatus status, String deviceType, Pageable pageable);
    
    List<Device> findByUserId(Integer userId);
}