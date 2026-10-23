package com.manacommunity.api.parking.anpr.repository;

import com.manacommunity.api.parking.anpr.entity.AnprGateEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AnprGateEventRepository extends JpaRepository<AnprGateEvent, Long> {

    List<AnprGateEvent> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    Page<AnprGateEvent> findByCommunityIdAndGateIdOrderByCreatedAtDesc(
            Long communityId, String gateId, Pageable pageable);

    Page<AnprGateEvent> findByCommunityIdOrderByCreatedAtDesc(Long communityId, Pageable pageable);

    List<AnprGateEvent> findByCommunityIdAndPlateNumberOrderByCreatedAtDesc(
            Long communityId, String plateNumber);

    @Query("SELECT e FROM AnprGateEvent e WHERE e.community.id = :communityId " +
           "AND e.barrierAction = 'HOLD' AND e.createdAt >= :since ORDER BY e.createdAt DESC")
    List<AnprGateEvent> findUnknownVehicleAlerts(
            @Param("communityId") Long communityId,
            @Param("since") LocalDateTime since);

    long countByCommunityIdAndBarrierActionAndCreatedAtAfter(
            Long communityId,
            AnprGateEvent.BarrierAction barrierAction,
            LocalDateTime after);
}
