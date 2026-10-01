package com.manacommunity.api.parking.ev.repository;

import com.manacommunity.api.parking.ev.entity.EvChargingSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvChargingSessionRepository extends JpaRepository<EvChargingSession, Long> {

    List<EvChargingSession> findByResidentIdOrderByStartedAtDesc(Long residentId);

    Page<EvChargingSession> findByCommunityIdOrderByStartedAtDesc(Long communityId, Pageable pageable);

    Optional<EvChargingSession> findFirstByChargerIdAndStatus(Long chargerId, EvChargingSession.SessionStatus status);

    Optional<EvChargingSession> findFirstByResidentIdAndStatus(Long residentId, EvChargingSession.SessionStatus status);
}