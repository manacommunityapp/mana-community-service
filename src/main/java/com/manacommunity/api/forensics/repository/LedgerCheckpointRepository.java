package com.manacommunity.api.forensics.repository;

import com.manacommunity.api.forensics.entity.LedgerCheckpointEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LedgerCheckpointRepository extends JpaRepository<LedgerCheckpointEntity, UUID> {

    Optional<LedgerCheckpointEntity> findTopBySocietyIdAndFlatIdAndRecordedCutoffLessThanEqualOrderByStreamVersionDesc(
            Long societyId, Long flatId, Instant recordedCutoff);

    List<LedgerCheckpointEntity> findBySocietyIdAndFlatIdOrderByStreamVersionDesc(Long societyId, Long flatId);
}
