package com.manacommunity.api.mobile.schoolbus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BusTripLogRepository extends JpaRepository<BusTripLog, Long> {
    List<BusTripLog> findByChildIdOrderByTripDateDescCreatedAtDesc(Long childId);
    Optional<BusTripLog> findFirstByChildIdAndTripDateOrderByCreatedAtDesc(Long childId, LocalDate date);
}
