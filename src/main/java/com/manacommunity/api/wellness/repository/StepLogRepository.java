package com.manacommunity.api.wellness.repository;

import com.manacommunity.api.wellness.entity.StepLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StepLogRepository extends JpaRepository<StepLog, Long> {

    Optional<StepLog> findByUserIdAndLogDate(Long userId, LocalDate logDate);

    List<StepLog> findByUserIdAndLogDateBetweenOrderByLogDateAsc(Long userId, LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(SUM(s.stepCount), 0) FROM StepLog s WHERE s.userId = :userId AND s.logDate = :date")
    Integer getTotalStepsForDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT s.logDate FROM StepLog s WHERE s.userId = :userId AND s.stepCount >= :minSteps ORDER BY s.logDate DESC")
    List<LocalDate> findDatesWhereStepsMetGoal(@Param("userId") Long userId, @Param("minSteps") int minSteps);
}
