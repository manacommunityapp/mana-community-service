package com.manacommunity.api.parking.repository;

import com.manacommunity.api.parking.entity.EvChargingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EvChargingSessionRepository extends JpaRepository<EvChargingSession, Long> {

    @Query("SELECT s FROM EvChargingSession s WHERE s.communityId = :communityId AND s.chargerId = :chargerId AND s.status = 'QUEUED' ORDER BY s.queuePosition ASC")
    List<EvChargingSession> findQueuedByCharger(@Param("communityId") Long communityId, @Param("chargerId") Long chargerId);

    @Query("SELECT s FROM EvChargingSession s WHERE s.communityId = :communityId AND s.status = 'CHARGING' AND s.startTime < :cutoffTime")
    List<EvChargingSession> findOverdueSessions(@Param("communityId") Long communityId, @Param("cutoffTime") Instant cutoffTime);

    @Query("SELECT s FROM EvChargingSession s WHERE s.communityId = :communityId AND s.flatNumber = :flatNumber AND s.billedToMaintenance = false AND s.status IN ('COMPLETED', 'AUTO_CUTOFF', 'OVERSTAY')")
    List<EvChargingSession> findUnbilledByFlat(@Param("communityId") Long communityId, @Param("flatNumber") String flatNumber);

    List<EvChargingSession> findByCommunityIdAndStatus(Long communityId, String status);

    @Query("SELECT s FROM EvChargingSession s WHERE s.communityId = :communityId AND s.status = 'QUEUED' ORDER BY s.queuePosition ASC")
    List<EvChargingSession> findAllQueued(@Param("communityId") Long communityId);
}
