package com.manacommunity.api.schoolbus.repository;

import com.manacommunity.api.schoolbus.entity.BusTripLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BusTripLogRepository extends JpaRepository<BusTripLog, Long> {
    List<BusTripLog> findByChildIdOrderByTripDateDesc(Long childId, Pageable pageable);
    Optional<BusTripLog> findByChildIdAndTripDate(Long childId, LocalDate tripDate);
}
