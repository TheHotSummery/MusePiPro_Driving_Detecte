package com.spacemit.musebackendv2.repository.v2;

import com.spacemit.musebackendv2.entity.v2.GpsDataV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * V2版本GPS数据Repository
 */
@Repository
public interface GpsDataV2Repository extends JpaRepository<GpsDataV2, Long> {
    
    List<GpsDataV2> findByDeviceId(String deviceId);
    
    List<GpsDataV2> findByDeviceIdAndTimestampBetween(String deviceId, Long startTime, Long endTime);
    
    List<GpsDataV2> findByTripId(String tripId);
    
    @Query(value = "SELECT * FROM gps_data WHERE device_id = :deviceId ORDER BY timestamp DESC LIMIT 1", nativeQuery = true)
    Optional<GpsDataV2> findLatestByDeviceId(@Param("deviceId") String deviceId);
    
    @Query("SELECT g FROM GpsDataV2 g WHERE g.deviceId = :deviceId AND g.timestamp BETWEEN :startTime AND :endTime ORDER BY g.timestamp ASC")
    List<GpsDataV2> findTrackByDeviceIdAndTimeRange(@Param("deviceId") String deviceId, @Param("startTime") Long startTime, @Param("endTime") Long endTime);
}
















