package com.manacommunity.api.forensics.repository;

import com.manacommunity.api.forensics.entity.BitemporalLedgerEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BitemporalLedgerEventRepository extends JpaRepository<BitemporalLedgerEventEntity, UUID> {

    Optional<BitemporalLedgerEventEntity> findBySocietyIdAndIdempotencyKey(Long societyId, String idempotencyKey);

    Optional<BitemporalLedgerEventEntity> findTopBySocietyIdAndFlatIdOrderByStreamVersionDesc(Long societyId, Long flatId);

    Optional<BitemporalLedgerEventEntity> findTopBySocietyIdOrderByGlobalSeqDesc(Long societyId);

    List<BitemporalLedgerEventEntity> findBySocietyIdAndFlatIdOrderByStreamVersionAsc(Long societyId, Long flatId);
}
