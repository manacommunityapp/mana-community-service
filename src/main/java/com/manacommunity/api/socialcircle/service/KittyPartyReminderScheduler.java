package com.manacommunity.api.socialcircle.service;

import com.manacommunity.api.model.Notification;
import com.manacommunity.api.model.NotificationCategory;
import com.manacommunity.api.model.NotificationType;
import com.manacommunity.api.model.ReferenceType;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.notification.orchestrator.NotificationDtos;
import com.manacommunity.api.notification.orchestrator.NotificationEnums;
import com.manacommunity.api.notification.orchestrator.UnifiedNotificationService;
import com.manacommunity.api.repository.NotificationRepository;
import com.manacommunity.api.socialcircle.entity.KittyPartyContribution;
import com.manacommunity.api.socialcircle.entity.KittyPartyCycle;
import com.manacommunity.api.socialcircle.entity.KittyPartyMember;
import com.manacommunity.api.socialcircle.repository.KittyPartyContributionRepository;
import com.manacommunity.api.socialcircle.repository.KittyPartyCycleRepository;
import com.manacommunity.api.socialcircle.repository.KittyPartyMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Automated Scheduled Daemon for Mana Kitty Party Payment Reminders.
 * <p>
 * Runs daily at 09:00 AM (configurable via {@code app.kitty-party.reminder-cron}).
 * Identifies active cycles, calculates period due dates, and dispatches multi-channel
 * notifications (In-app, Push, WhatsApp, SMS via UnifiedNotificationService) to unpaid
 * circle members 3 days before due date, on the due date, and during grace period.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KittyPartyReminderScheduler {

    private final KittyPartyCycleRepository cycleRepository;
    private final KittyPartyContributionRepository contributionRepository;
    private final KittyPartyMemberRepository memberRepository;
    private final KittyPartyLedgerService ledgerService;
    private final NotificationRepository notificationRepository;

    @Autowired(required = false)
    private UnifiedNotificationService unifiedNotificationService;

    /**
     * Daily background job at 09:00 AM.
     */
    @Scheduled(cron = "${app.kitty-party.reminder-cron:0 0 9 * * ?}")
    @Transactional
    public int runAutomatedReminders() {
        log.info("Starting KittyParty automated scheduled payment reminder job...");
        List<KittyPartyCycle> activeCycles = cycleRepository.findByStatus("ACTIVE");
        if (activeCycles.isEmpty()) {
            log.info("No active kitty party cycles found for reminder processing.");
            return 0;
        }

        LocalDate today = LocalDate.now();
        int totalRemindersSent = 0;

        for (KittyPartyCycle cycle : activeCycles) {
            try {
                int currentPeriod = ledgerService.computeCurrentPeriod(cycle);
                LocalDate dueDate = ledgerService.computePeriodDueDate(cycle.getStartDate(), currentPeriod);
                long daysUntilDue = ChronoUnit.DAYS.between(today, dueDate);

                // Reminder cadence:
                // 1. 3-day advance notice (daysUntilDue == 3)
                // 2. Due today (daysUntilDue == 0)
                // 3. Overdue within grace period (daysUntilDue < 0 && daysUntilDue >= -3)
                boolean shouldRemind = (daysUntilDue == 3 || daysUntilDue == 0 || (daysUntilDue < 0 && daysUntilDue >= -3));

                if (!shouldRemind) {
                    continue;
                }

                String urgencyPrefix = daysUntilDue == 0 ? "⚠️ DUE TODAY: " :
                        daysUntilDue < 0 ? "🚨 OVERDUE: " : "👛 UPCOMING DUE: ";

                List<KittyPartyMember> activeMembers = memberRepository.findByCircleIdAndMembershipStatus(
                        cycle.getCircle().getId(), "ACTIVE");
                List<KittyPartyContribution> contributions = contributionRepository.findByCycleIdAndPeriodNumber(
                        cycle.getId(), currentPeriod);

                Set<Long> paidUserIds = contributions.stream()
                        .filter(c -> "PAID".equalsIgnoreCase(c.getPaymentStatus()) || "VERIFIED".equalsIgnoreCase(c.getPaymentStatus()))
                        .map(c -> c.getMemberUser().getId())
                        .collect(Collectors.toSet());

                for (KittyPartyMember member : activeMembers) {
                    if (!paidUserIds.contains(member.getUser().getId())) {
                        dispatchReminder(cycle, member, currentPeriod, urgencyPrefix, dueDate);
                        totalRemindersSent++;
                    }
                }
            } catch (Exception e) {
                log.error("Error processing reminders for cycle id {}: {}", cycle.getId(), e.getMessage(), e);
            }
        }

        log.info("Completed KittyParty scheduled payment reminders: {} notifications dispatched.", totalRemindersSent);
        return totalRemindersSent;
    }

    private void dispatchReminder(KittyPartyCycle cycle, KittyPartyMember member, int periodNumber, String urgencyPrefix, LocalDate dueDate) {
        String title = urgencyPrefix + "Kitty Pool Period " + periodNumber;
        String body = "Hi " + member.getUser().getFullName() + ", your monthly contribution of ₹" +
                cycle.getContributionAmount() + " for " + cycle.getCycleName() +
                " is due on " + dueDate + ". Tap to record or pay via UPI.";
        String actionUrl = "/kitty-party/" + cycle.getCircle().getId();

        // 1. In-App Notification
        try {
            Notification notification = Notification.builder()
                    .user(member.getUser())
                    .community(cycle.getCircle().getCommunity())
                    .type(NotificationType.GENERAL)
                    .category(NotificationCategory.GENERAL)
                    .title(title)
                    .body(body)
                    .referenceType(ReferenceType.COMMUNITY)
                    .referenceId(cycle.getCircle().getCommunity().getId())
                    .actionUrl(actionUrl)
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.warn("Failed to save in-app notification for user {}: {}", member.getUser().getId(), e.getMessage());
        }

        // 2. Multi-channel orchestration (Push, WhatsApp, SMS per resident preferences)
        if (unifiedNotificationService != null) {
            try {
                unifiedNotificationService.orchestrate(
                        NotificationDtos.UnifiedNotificationRequest.builder()
                                .recipientUserId(member.getUser().getId())
                                .category(NotificationEnums.NotificationCategory.FINANCIAL_BILLING)
                                .priority(NotificationEnums.NotificationPriority.HIGH)
                                .title(title)
                                .body(body)
                                .actionUrl(actionUrl)
                                .build()
                );
            } catch (Exception e) {
                log.warn("Failed to dispatch unified notification for user {}: {}", member.getUser().getId(), e.getMessage());
            }
        }
    }
}
