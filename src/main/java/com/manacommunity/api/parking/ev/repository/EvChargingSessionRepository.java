package com.manacommunity.api.parking.ev.repository;

import com.manacommunity.api.parking.ev.entity.EvChargingSession;
import com.manacommunity.api.parking.ev.enums.EvSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvChargingSessionRepository extends JpaRepository<EvChargingSession, Long> {
    List<EvChargingSession> findByResidentIdOrderByStartTimeDesc(Long residentId);
    Optional<EvChargingSession> findByStationIdAndStatus(Long stationId, EvSessionStatus status);
    List<EvChargingSession> findByCommunityIdOrderByStartTimeDesc(Long communityId);
}
