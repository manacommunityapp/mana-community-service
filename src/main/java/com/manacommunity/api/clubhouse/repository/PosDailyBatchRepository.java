package com.manacommunity.api.clubhouse.repository;

import com.manacommunity.api.clubhouse.entity.PosDailyBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PosDailyBatchRepository extends JpaRepository<PosDailyBatchEntity, UUID> {
    Optional<PosDailyBatchEntity> findTopBySocietyIdAndTerminalIdAndBusinessDateOrderByBatchSeqDesc(
            Long societyId, String terminalId, LocalDate businessDate);
    List<PosDailyBatchEntity> findBySocietyIdOrderByClosedAtDesc(Long societyId);
}
