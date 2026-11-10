package com.manacommunity.api.fresh.processing.repository;

import com.manacommunity.api.fresh.processing.model.FreshBatchQualityCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FreshBatchQualityCheckRepository extends JpaRepository<FreshBatchQualityCheck, Long> {
    List<FreshBatchQualityCheck> findByBatchIdOrderByCheckedAtAsc(Long batchId);
}