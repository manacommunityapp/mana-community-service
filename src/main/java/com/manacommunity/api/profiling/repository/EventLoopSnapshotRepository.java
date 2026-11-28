package com.manacommunity.api.profiling.repository;

import com.manacommunity.api.profiling.entity.EventLoopSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventLoopSnapshotRepository extends JpaRepository<EventLoopSnapshot, Long> {
    List<EventLoopSnapshot> findByServiceIdOrderBySnapshotTimestampDesc(String serviceId);
    List<EventLoopSnapshot> findByHealthOrderBySnapshotTimestampDesc(EventLoopSnapshot.LoopHealth health);
}
