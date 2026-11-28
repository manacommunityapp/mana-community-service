package com.manacommunity.api.recovery.repository;

import com.manacommunity.api.recovery.entity.RfidTagSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RfidTagSnapshotRepository extends JpaRepository<RfidTagSnapshotEntity, String> {

    List<RfidTagSnapshotEntity> findBySocietyIdOrderByTagIdAsc(Long societyId);
}
