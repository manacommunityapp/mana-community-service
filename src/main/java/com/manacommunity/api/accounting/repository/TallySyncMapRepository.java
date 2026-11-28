package com.manacommunity.api.accounting.repository;

import com.manacommunity.api.accounting.entity.TallySyncMapEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TallySyncMapRepository extends JpaRepository<TallySyncMapEntity, Long> {

    Optional<TallySyncMapEntity> findBySocietyIdAndTallyGuid(Long societyId, UUID tallyGuid);

    Optional<TallySyncMapEntity> findBySocietyIdAndVoucherId(Long societyId, UUID voucherId);

    List<TallySyncMapEntity> findBySocietyIdAndSyncStatusOrderByIdAsc(Long societyId, String syncStatus);

    long countBySocietyIdAndSyncStatus(Long societyId, String syncStatus);
}
