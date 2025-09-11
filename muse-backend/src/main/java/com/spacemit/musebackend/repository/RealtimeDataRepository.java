package com.spacemit.musebackend.repository;

import com.spacemit.musebackend.entity.RealtimeData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RealtimeDataRepository extends JpaRepository<RealtimeData, Integer> {
    List<RealtimeData> findByDeviceIdAndTimestampBetween(String deviceId, LocalDateTime start, LocalDateTime end);
    List<RealtimeData> findByDeviceIdOrderByTimestampDesc(String deviceId);
    
    List<RealtimeData> findByDeviceIdAndTimestampBetweenOrderByTimestampAsc(String deviceId, LocalDateTime start, LocalDateTime end);
    
    List<RealtimeData> findByDeviceIdOrderByTimestampAsc(String deviceId);
    
    Optional<RealtimeData> findTopByDeviceIdOrderByTimestampDesc(String deviceId);
}