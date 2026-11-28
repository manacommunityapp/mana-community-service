package com.manacommunity.api.profiling.repository;

import com.manacommunity.api.profiling.entity.ThreadPoolSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThreadPoolSnapshotRepository extends JpaRepository<ThreadPoolSnapshot, Long> {
    List<ThreadPoolSnapshot> findByServiceIdOrderBySnapshotTimestampDesc(String serviceId);
    List<ThreadPoolSnapshot> findByHealthOrderBySnapshotTimestampDesc(ThreadPoolSnapshot.PoolHealth health);
}
