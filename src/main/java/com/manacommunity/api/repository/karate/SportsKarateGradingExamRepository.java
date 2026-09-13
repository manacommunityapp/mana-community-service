package com.manacommunity.api.repository.karate;

import com.manacommunity.api.model.karate.SportsKarateGradingExam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SportsKarateGradingExamRepository extends JpaRepository<SportsKarateGradingExam, Long> {

    List<SportsKarateGradingExam> findByBatchIdOrderByScheduledDateAsc(Long batchId);

    List<SportsKarateGradingExam> findByBatchIdAndStatus(Long batchId, SportsKarateGradingExam.ExamStatus status);

    /** Exams scheduled on the given date that have not had reminders sent (for the T-3 days scheduler). */
    @Query("""
            SELECT e FROM SportsKarateGradingExam e
            WHERE e.scheduledDate = :targetDate
              AND e.status = 'SCHEDULED'
              AND e.reminderSent = false
            """)
    List<SportsKarateGradingExam> findExamsPendingReminder(@Param("targetDate") LocalDate targetDate);
}
