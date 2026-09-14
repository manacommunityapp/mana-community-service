package com.manacommunity.api.scheduler;

import com.manacommunity.api.model.karate.SportsKarateEnrollment;
import com.manacommunity.api.model.karate.SportsKarateGradingExam;
import com.manacommunity.api.repository.karate.SportsKarateEnrollmentRepository;
import com.manacommunity.api.repository.karate.SportsKarateGradingExamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Sends a grading-exam reminder 3 days before the exam to all eligible
 * active enrollments in the batch. Marks reminderSent=true so each exam
 * is reminded exactly once.
 *
 * Runs daily at 09:00 AM.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SportsKarateExamReminderScheduler {

    private static final int REMINDER_DAYS_BEFORE = 3;

    private final SportsKarateGradingExamRepository examRepo;
    private final SportsKarateEnrollmentRepository enrollmentRepo;

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendExamReminders() {
        LocalDate targetDate = LocalDate.now().plusDays(REMINDER_DAYS_BEFORE);
        List<SportsKarateGradingExam> exams = examRepo.findExamsPendingReminder(targetDate);
        if (exams.isEmpty()) return;

        log.info("Karate exam reminder: {} exam(s) in {} days", exams.size(), REMINDER_DAYS_BEFORE);

        for (SportsKarateGradingExam exam : exams) {
            try {
                List<SportsKarateEnrollment> eligible = enrollmentRepo
                        .findByBatchIdAndStatusAndGradingEligibleTrue(
                                exam.getBatch().getId(), SportsKarateEnrollment.EnrollmentStatus.ACTIVE);

                for (SportsKarateEnrollment enrollment : eligible) {
                    log.debug("Exam reminder → student {} for exam {} (target belt: {})",
                            enrollment.getStudent().getId(), exam.getId(), exam.getTargetBelt().getName());
                    // TODO: wire to push/notification service
                    //   notificationService.send(enrollment.getStudent(),
                    //       "Your Karate grading exam for " + exam.getTargetBelt().getName()
                    //       + " is on " + exam.getScheduledDate());
                }

                exam.setReminderSent(true);
                examRepo.save(exam);

            } catch (Exception ex) {
                log.error("Failed to send exam reminder for exam {}: {}", exam.getId(), ex.getMessage(), ex);
            }
        }
    }
}
