package com.manacommunity.api.profiling.repository;

import com.manacommunity.api.profiling.entity.BrowserMemorySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrowserMemorySnapshotRepository extends JpaRepository<BrowserMemorySnapshot, Long> {
    List<BrowserMemorySnapshot> findByPortalNameOrderBySnapshotTimestampDesc(String portalName);
    List<BrowserMemorySnapshot> findByHealthOrderBySnapshotTimestampDesc(BrowserMemorySnapshot.MemoryHealth health);
}
