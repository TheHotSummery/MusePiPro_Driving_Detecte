package com.spacemit.musebackend.repository;

import com.spacemit.musebackend.entity.GpsData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GpsDataRepository extends JpaRepository<GpsData, Integer> {
    List<GpsData> findByDeviceIdAndTimestampBetween(String deviceId, LocalDateTime start, LocalDateTime end);
    List<GpsData> findByDeviceIdOrderByTimestampDesc(String deviceId);
    List<GpsData> findByDeviceIdAndTimestampBetweenOrderByTimestampAsc(String deviceId, LocalDateTime start, LocalDateTime end);
    List<GpsData> findByDeviceIdOrderByTimestampAsc(String deviceId);
    Optional<GpsData> findTopByDeviceIdOrderByTimestampDesc(String deviceId);
    
    // 根据疲劳等级查询
    List<GpsData> findByFatigueLevel(GpsData.FatigueLevel fatigueLevel);
    List<GpsData> findByDeviceIdAndFatigueLevel(String deviceId, GpsData.FatigueLevel fatigueLevel);
    
    // 根据时间范围查询疲劳数据
    List<GpsData> findByDeviceIdAndFatigueLevelAndTimestampBetween(
        String deviceId, GpsData.FatigueLevel fatigueLevel, LocalDateTime start, LocalDateTime end);
}
