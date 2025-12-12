package com.spacemit.musebackendv2.repository.v2;

import com.spacemit.musebackendv2.entity.v2.EventDataV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * V2版本事件数据Repository
 */
@Repository
public interface EventDataV2Repository extends JpaRepository<EventDataV2, Long> {
    
    Optional<EventDataV2> findByEventId(String eventId);
    
    List<EventDataV2> findByDeviceId(String deviceId);
    
    List<EventDataV2> findByDriverId(String driverId);
    
    List<EventDataV2> findByDeviceIdAndTimestampBetween(String deviceId, Long startTime, Long endTime);
    
    List<EventDataV2> findByDriverIdAndTimestampBetween(String driverId, Long startTime, Long endTime);
    
    @Query("SELECT e FROM EventDataV2 e WHERE e.timestamp BETWEEN :startTime AND :endTime")
    List<EventDataV2> findByTimestampBetween(@Param("startTime") Long startTime, @Param("endTime") Long endTime);
    
    @Query("SELECT COUNT(e) FROM EventDataV2 e WHERE e.deviceId = :deviceId AND e.timestamp BETWEEN :startTime AND :endTime")
    Long countByDeviceIdAndTimestampBetween(@Param("deviceId") String deviceId, @Param("startTime") Long startTime, @Param("endTime") Long endTime);
}

