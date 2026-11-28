package com.manacommunity.api.recovery.repository;

import com.manacommunity.api.recovery.entity.GstBackfillItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GstBackfillItemRepository extends JpaRepository<GstBackfillItemEntity, UUID> {

    Optional<GstBackfillItemEntity> findByJobIdAndOriginalVoucherId(UUID jobId, UUID originalVoucherId);

    List<GstBackfillItemEntity> findByJobIdOrderBySocietyIdAscFlatIdAsc(UUID jobId);
}
