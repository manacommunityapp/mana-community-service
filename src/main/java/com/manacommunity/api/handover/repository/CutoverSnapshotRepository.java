package com.manacommunity.api.handover.repository;

import com.manacommunity.api.handover.entity.CutoverSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CutoverSnapshotRepository extends JpaRepository<CutoverSnapshot, Long> {

    Optional<CutoverSnapshot> findBySnapshotCode(String snapshotCode);

    Optional<CutoverSnapshot> findTopByProjectIdOrderByCreatedAtDesc(Long projectId);
}
