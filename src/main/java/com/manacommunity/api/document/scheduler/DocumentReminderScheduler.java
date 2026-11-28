package com.manacommunity.api.document.scheduler;

import com.manacommunity.api.document.entity.Document;
import com.manacommunity.api.document.repository.DocumentRepository;
import com.manacommunity.api.notification.orchestrator.NotificationDtos.UnifiedNotificationRequest;
import com.manacommunity.api.notification.orchestrator.NotificationEnums.NotificationCategory;
import com.manacommunity.api.notification.orchestrator.NotificationEnums.NotificationPriority;
import com.manacommunity.api.notification.orchestrator.UnifiedNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentReminderScheduler {

    private final DocumentRepository documentRepo;
    private final ObjectProvider<UnifiedNotificationService> notificationServiceProvider;

    @Scheduled(cron = "0 0 8 * * *")
    public void sendExpiryReminders() {
        Instant deadline = Instant.now().plus(30, ChronoUnit.DAYS);
        List<Document> expiring = documentRepo.findExpiringSoon(deadline);
        UnifiedNotificationService notif = notificationServiceProvider.getIfAvailable();

        for (Document doc : expiring) {
            log.info("[DocReminder] Document {} '{}' expires at {} — notify owner {}",
                    doc.getId(), doc.getTitle(), doc.getExpiresAt(), doc.getOwnerUserId());

            if (doc.getOwnerUserId() != null && notif != null) {
                try {
                    notif.orchestrate(UnifiedNotificationRequest.builder()
                            .recipientUserId(doc.getOwnerUserId())
                            .category(NotificationCategory.COMMUNITY_NOTICE)
                            .priority(NotificationPriority.NORMAL)
                            .title("Document Expiring Soon: " + doc.getTitle())
                            .body(String.format("Your document '%s' is set to expire on %s. Please upload an updated renewal copy.",
                                    doc.getTitle(), doc.getExpiresAt()))
                            .data(Map.of("documentId", doc.getId(), "expiresAt", doc.getExpiresAt().toString()))
                            .actionUrl("/resident/documents/" + doc.getId())
                            .build());
                } catch (Exception e) {
                    log.warn("[DocReminder] Failed to send reminder for document {}: {}", doc.getId(), e.getMessage());
                }
            }
        }
    }
}
