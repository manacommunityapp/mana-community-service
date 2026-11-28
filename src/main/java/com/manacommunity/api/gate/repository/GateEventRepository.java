package com.manacommunity.api.gate.repository;

import com.manacommunity.api.gate.model.GateEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface GateEventRepository extends JpaRepository<GateEvent, Long> {

    Page<GateEvent> findByCommunityIdOrderByCreatedAtDesc(Long communityId, Pageable pageable);

    Page<GateEvent> findByCommunityIdAndGateIdOrderByCreatedAtDesc(
            Long communityId, String gateId, Pageable pageable);

    Page<GateEvent> findByCommunityIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long communityId, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
