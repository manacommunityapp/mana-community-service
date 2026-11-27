package com.manacommunity.api.visitor.repository;

import com.manacommunity.api.visitor.entity.WhitelistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WhitelistRepository extends JpaRepository<WhitelistEntry, Long> {
    List<WhitelistEntry> findByResidentId(Long residentId);

    List<WhitelistEntry> findByCommunityIdAndIsActiveTrue(Long communityId);

    @Query("""
           SELECT w FROM WhitelistEntry w 
           WHERE w.community.id = :communityId 
             AND w.isActive = true
             AND REPLACE(REPLACE(UPPER(w.vehicleNumber), ' ', ''), '-', '') = :normalizedPlate
           """)
    List<WhitelistEntry> findActiveByCommunityAndNormalizedVehicle(
            @Param("communityId") Long communityId,
            @Param("normalizedPlate") String normalizedPlate);
}
