package com.spacemit.musebackendv2.repository.v2;

import com.spacemit.musebackendv2.entity.v2.TeamV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * V2版本车队Repository
 */
@Repository
public interface TeamV2Repository extends JpaRepository<TeamV2, Long> {
    
    Optional<TeamV2> findByTeamId(String teamId);
    
    Optional<TeamV2> findByTeamIdAndDeletedAtIsNull(String teamId);
    
    @Query("SELECT t FROM TeamV2 t WHERE t.deletedAt IS NULL")
    List<TeamV2> findAllActive();
}
















