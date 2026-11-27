package com.manacommunity.api.accounting.repository;

import com.manacommunity.api.accounting.entity.TallySyncConflictEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TallySyncConflictRepository extends JpaRepository<TallySyncConflictEntity, UUID> {

    List<TallySyncConflictEntity> findBySocietyIdAndResolutionStatusOrderByCreatedAtDesc(
            Long societyId, String resolutionStatus);

    long countBySocietyIdAndResolutionStatus(Long societyId, String resolutionStatus);
}
