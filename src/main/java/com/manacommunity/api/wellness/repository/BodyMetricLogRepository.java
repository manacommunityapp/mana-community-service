package com.manacommunity.api.wellness.repository;

import com.manacommunity.api.wellness.entity.BodyMetricLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BodyMetricLogRepository extends JpaRepository<BodyMetricLog, Long> {
    List<BodyMetricLog> findByUserIdOrderByLoggedDateDesc(Long userId);
    List<BodyMetricLog> findByUserIdAndLoggedDateBetweenOrderByLoggedDateAsc(Long userId, LocalDate start, LocalDate end);
    Optional<BodyMetricLog> findByUserIdAndLoggedDate(Long userId, LocalDate loggedDate);
    Optional<BodyMetricLog> findFirstByUserIdOrderByLoggedDateDesc(Long userId);
}
