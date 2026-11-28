package com.manacommunity.api.profiling.repository;

import com.manacommunity.api.profiling.entity.GcEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GcEventRepository extends JpaRepository<GcEvent, Long> {
    List<GcEvent> findByServiceIdOrderByEventTimestampDesc(String serviceId);
    List<GcEvent> findByStopTheWorldTrueOrderByPauseMillisDesc();
}
