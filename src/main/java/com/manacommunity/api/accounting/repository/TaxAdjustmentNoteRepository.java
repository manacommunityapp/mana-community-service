package com.manacommunity.api.accounting.repository;

import com.manacommunity.api.accounting.entity.TaxAdjustmentNoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaxAdjustmentNoteRepository extends JpaRepository<TaxAdjustmentNoteEntity, UUID> {
    List<TaxAdjustmentNoteEntity> findByBatchId(UUID batchId);
}
