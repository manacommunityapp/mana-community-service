package com.manacommunity.api.scheduler;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.SportsEventRegistration;
import com.manacommunity.api.model.SportsNotificationScheduler;
import com.manacommunity.api.repository.SportsEventRegistrationRepository;
import com.manacommunity.api.repository.SportsNotificationSchedulerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sends due event notifications to a tournament's confirmed registrants.
 * Consolidated onto the rich {@link SportsNotificationScheduler} (which carries
 * offset/priority/trigger metadata and covers the old use case); the legacy
 * EventNotificationSchedule loop was retired.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final SportsEventRegistrationRepository regRepo;
    private final PushNotificationService pushService;
    private final SportsNotificationSchedulerRepository schedulerRepo;

    @Scheduled(fixedDelay = 60_000)  // every 60 seconds
    @Transactional
    public void sendPendingNotifications() {
        List<SportsNotificationScheduler> pending =
                schedulerRepo.findByNotifyAtBeforeAndSentFalseAndEnabledTrue(LocalDateTime.now());

        for (SportsNotificationScheduler notif : pending) {
            try {
                List<AppUser> recipients = regRepo
                        .findByEventIdAndStatus(
                                notif.getEvent().getId(),
                                SportsEventRegistration.RegistrationStatus.CONFIRMED)
                        .stream().map(SportsEventRegistration::getUser).toList();

                if (!recipients.isEmpty()) {
                    pushService.sendBulk(recipients, notif.getTitle(), notif.getBody());
                }
                notif.setSent(true);
                schedulerRepo.save(notif);
            } catch (Exception e) {
                log.error("Failed to send notification id={}", notif.getId(), e);
            }
        }
    }
}
