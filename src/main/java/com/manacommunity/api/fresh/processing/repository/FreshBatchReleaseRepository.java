package com.manacommunity.api.fresh.processing.repository;

import com.manacommunity.api.fresh.processing.model.FreshBatchRelease;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FreshBatchReleaseRepository extends JpaRepository<FreshBatchRelease, Long> {
    Optional<FreshBatchRelease> findByBatchId(Long batchId);
}