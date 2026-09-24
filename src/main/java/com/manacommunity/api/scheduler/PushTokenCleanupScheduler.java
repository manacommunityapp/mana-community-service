package com.manacommunity.api.scheduler;

import com.manacommunity.api.repository.PushTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Removes push tokens that have been deactivated for more than 30 days.
 * Keeps the table lean so lookup queries stay fast.
 *
 * Runs at 03:00 AM every Sunday. Adjust the cron expression in
 * application.yaml if needed:
 *
 *   app.push.cleanup-cron: "0 0 3 * * SUN"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushTokenCleanupScheduler {

    private final PushTokenRepository tokenRepo;

    @Scheduled(cron = "${app.push.cleanup-cron:0 0 3 * * SUN}")
    @Transactional
    public void cleanupStaleTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        int deleted = tokenRepo.deleteInactiveBefore(cutoff);
        if (deleted > 0) {
            log.info("[PushCleanup] Removed {} stale push token(s) older than 30 days", deleted);
        }
    }
}
