package com.manacommunity.api.repository.karate;

import com.manacommunity.api.model.karate.SportsKarateClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SportsKarateClassRepository extends JpaRepository<SportsKarateClass, Long> {

    List<SportsKarateClass> findByBatchIdOrderByScheduledDateAsc(Long batchId);

    List<SportsKarateClass> findByBatchIdAndStatusOrderByScheduledDateAsc(
            Long batchId, SportsKarateClass.ClassStatus status);

    List<SportsKarateClass> findByBatchIdAndScheduledDateBetweenOrderByScheduledDateAsc(
            Long batchId, LocalDate from, LocalDate to);

    /** Classes scheduled for today that haven't sent a reminder yet (for morning scheduler). */
    @Query("""
            SELECT c FROM SportsKarateClass c
            WHERE c.scheduledDate = :today
              AND c.status = 'SCHEDULED'
              AND c.reminderSent = false
            """)
    List<SportsKarateClass> findTodayClassesPendingReminder(@Param("today") LocalDate today);

    long countByBatchIdAndStatus(Long batchId, SportsKarateClass.ClassStatus status);

    /** Checks if a class session already exists for a batch on a given date. */
    boolean existsByBatchIdAndScheduledDate(Long batchId, LocalDate scheduledDate);
}
