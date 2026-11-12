package com.manacommunity.api.schoolbus.repository;

import com.manacommunity.api.schoolbus.entity.BusTripLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BusTripLogRepository extends JpaRepository<BusTripLog, Long> {
    List<BusTripLog> findByChildIdOrderByTripDateDesc(Long childId, Pageable pageable);
}
