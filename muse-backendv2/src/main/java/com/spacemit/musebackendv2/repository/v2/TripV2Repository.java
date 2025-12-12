package com.spacemit.musebackendv2.repository.v2;

import com.spacemit.musebackendv2.entity.v2.TripV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * V2版本行程Repository
 */
@Repository
public interface TripV2Repository extends JpaRepository<TripV2, Long> {
    
    Optional<TripV2> findByTripId(String tripId);
    
    List<TripV2> findByDeviceId(String deviceId);
    
    List<TripV2> findByDriverId(String driverId);
    
    List<TripV2> findByDeviceIdAndStartTimeBetween(String deviceId, Long startTime, Long endTime);
    
    List<TripV2> findByDriverIdAndStartTimeBetween(String driverId, Long startTime, Long endTime);
    
    @Query("SELECT t FROM TripV2 t WHERE t.startTime BETWEEN :startTime AND :endTime")
    List<TripV2> findByTimeRange(@Param("startTime") Long startTime, @Param("endTime") Long endTime);
    
    @Query("SELECT t FROM TripV2 t WHERE t.deviceId = :deviceId AND t.status = 'ONGOING' ORDER BY t.startTime DESC")
    List<TripV2> findOngoingTripsByDeviceId(@Param("deviceId") String deviceId);
}

