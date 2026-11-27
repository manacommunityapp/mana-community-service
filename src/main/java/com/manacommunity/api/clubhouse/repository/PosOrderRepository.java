package com.manacommunity.api.clubhouse.repository;

import com.manacommunity.api.clubhouse.entity.PosOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PosOrderRepository extends JpaRepository<PosOrderEntity, UUID> {
    Optional<PosOrderEntity> findBySocietyIdAndClientTxUuid(Long societyId, String clientTxUuid);
    Optional<PosOrderEntity> findTopBySocietyIdOrderByCreatedAtDesc(Long societyId);
    List<PosOrderEntity> findBySocietyIdOrderByOrderedAtDesc(Long societyId);
    List<PosOrderEntity> findBySocietyIdAndTerminalIdAndBatchIdIsNullAndOrderedAtBetween(
            Long societyId, String terminalId, Instant from, Instant to);
}
