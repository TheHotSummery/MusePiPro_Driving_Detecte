package com.spacemit.musebackendv2.repository.v2;

import com.spacemit.musebackendv2.entity.v2.DriverV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * V2版本驾驶员Repository
 */
@Repository
public interface DriverV2Repository extends JpaRepository<DriverV2, Long> {
    
    Optional<DriverV2> findByDriverId(String driverId);
    
    Optional<DriverV2> findByDriverIdAndDeletedAtIsNull(String driverId);
    
    List<DriverV2> findByTeamId(String teamId);
    
    List<DriverV2> findByStatus(DriverV2.DriverStatus status);
    
    @Query("SELECT d FROM DriverV2 d WHERE d.deletedAt IS NULL")
    List<DriverV2> findAllActive();
}
















