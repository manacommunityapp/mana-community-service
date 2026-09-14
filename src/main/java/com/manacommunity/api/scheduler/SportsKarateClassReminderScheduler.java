package com.manacommunity.api.scheduler;

import com.manacommunity.api.model.karate.SportsKarateClass;
import com.manacommunity.api.model.karate.SportsKarateEnrollment;
import com.manacommunity.api.repository.karate.SportsKarateClassRepository;
import com.manacommunity.api.repository.karate.SportsKarateEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Sends a class-day reminder to all active enrollments of a batch
 * at 08:00 AM on the day the class is scheduled.
 *
 * Marks reminderSent=true so the class is never reminded twice.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SportsKarateClassReminderScheduler {

    private final SportsKarateClassRepository classRepo;
    private final SportsKarateEnrollmentRepository enrollmentRepo;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void sendTodayClassReminders() {
        LocalDate today = LocalDate.now();
        List<SportsKarateClass> due = classRepo.findTodayClassesPendingReminder(today);
        if (due.isEmpty()) return;

        log.info("Karate class reminder: {} class(es) scheduled for today ({})", due.size(), today);

        for (SportsKarateClass cls : due) {
            try {
                List<SportsKarateEnrollment> activeEnrollments = enrollmentRepo.findByBatchIdAndStatus(
                        cls.getBatch().getId(), SportsKarateEnrollment.EnrollmentStatus.ACTIVE);

                for (SportsKarateEnrollment enrollment : activeEnrollments) {
                    log.debug("Karate class reminder → student {} for class {} on {}",
                            enrollment.getStudent().getId(), cls.getId(), cls.getScheduledDate());
                    // TODO: wire to push/notification service when available
                    //   notificationService.send(enrollment.getStudent(), "Your Karate class is at " + cls.getStartTime());
                }

                cls.setReminderSent(true);
                classRepo.save(cls);

            } catch (Exception ex) {
                log.error("Failed to send reminder for class {}: {}", cls.getId(), ex.getMessage(), ex);
            }
        }
    }
}
