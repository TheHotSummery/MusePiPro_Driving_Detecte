package com.spacemit.musebackendv2.repository.v2;

import com.spacemit.musebackendv2.entity.v2.StatusDataV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * V2版本状态数据Repository
 */
@Repository
public interface StatusDataV2Repository extends JpaRepository<StatusDataV2, Long> {
    
    List<StatusDataV2> findByDeviceId(String deviceId);
    
    @Query("SELECT s FROM StatusDataV2 s WHERE s.deviceId = :deviceId AND s.timestamp BETWEEN :startTime AND :endTime ORDER BY s.timestamp ASC")
    List<StatusDataV2> findByDeviceIdAndTimestampBetween(@Param("deviceId") String deviceId, @Param("startTime") Long startTime, @Param("endTime") Long endTime);
    
    @Query("SELECT s FROM StatusDataV2 s WHERE s.timestamp BETWEEN :startTime AND :endTime ORDER BY s.timestamp ASC")
    List<StatusDataV2> findByTimestampBetween(@Param("startTime") Long startTime, @Param("endTime") Long endTime);
    
    @Query("SELECT s FROM StatusDataV2 s WHERE s.deviceId = :deviceId ORDER BY s.timestamp DESC")
    List<StatusDataV2> findLatestByDeviceId(@Param("deviceId") String deviceId);
    
    @Query(value = "SELECT * FROM status_data WHERE device_id = :deviceId ORDER BY timestamp DESC LIMIT 1", nativeQuery = true)
    Optional<StatusDataV2> findLatestOneByDeviceId(@Param("deviceId") String deviceId);
}

