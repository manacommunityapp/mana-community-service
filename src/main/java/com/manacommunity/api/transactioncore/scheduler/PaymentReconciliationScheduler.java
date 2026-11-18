package com.manacommunity.api.transactioncore.scheduler;

import com.manacommunity.api.transactioncore.service.TransactionReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationScheduler {

    private final TransactionReconciliationService reconciliationService;

    // Runs daily at 02:00 UTC across all communities (communityId = null).
    @Scheduled(cron = "0 0 2 * * *")
    public void runDailyReconciliation() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("[SCHEDULER] Starting daily payment reconciliation for date={}", yesterday);
        try {
            reconciliationService.runDailyReconciliation(yesterday, null);
            log.info("[SCHEDULER] Daily payment reconciliation completed for date={}", yesterday);
        } catch (Exception e) {
            log.error("[SCHEDULER] Daily payment reconciliation failed for date={}: {}", yesterday, e.getMessage(), e);
        }
    }
}
