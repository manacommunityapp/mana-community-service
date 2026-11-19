package com.manacommunity.api.document.scheduler;

import com.manacommunity.api.document.entity.Document;
import com.manacommunity.api.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentReminderScheduler {

    private final DocumentRepository documentRepo;

    @Scheduled(cron = "0 0 8 * * *")
    public void sendExpiryReminders() {
        Instant deadline = Instant.now().plus(30, ChronoUnit.DAYS);
        List<Document> expiring = documentRepo.findExpiringSoon(deadline);
        for (Document doc : expiring) {
            log.info("[DocReminder] Document {} '{}' expires at {} — notify owner {}",
                    doc.getId(), doc.getTitle(), doc.getExpiresAt(), doc.getOwnerUserId());
            // TODO: wire into NotificationService when available
        }
    }
}
