package com.manacommunity.api.parking.repository;

import com.manacommunity.api.parking.entity.ParkingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {

    List<ParkingSession> findByCommunityIdAndExitTimeIsNullOrderByEntryTimeDesc(Long communityId);

    Optional<ParkingSession> findTopByCommunityIdAndVehicleNumberAndExitTimeIsNullOrderByEntryTimeDesc(
            Long communityId, String vehicleNumber);

    long countByCommunityIdAndExitTimeIsNull(Long communityId);

    @Query("SELECT s FROM ParkingSession s WHERE s.community.id = :cid " +
           "AND s.exitTime IS NULL AND s.entryTime < :threshold")
    List<ParkingSession> findPotentialOverstays(
            @Param("cid") Long communityId,
            @Param("threshold") Instant threshold);
}
