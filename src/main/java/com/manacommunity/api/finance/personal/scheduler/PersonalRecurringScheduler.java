package com.manacommunity.api.finance.personal.scheduler;

import com.manacommunity.api.finance.personal.service.PersonalFinanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nightly background scheduler to automatically execute due recurring personal transactions
 * and advance rule nextDueDate.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "mana.personal-finance.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class PersonalRecurringScheduler {

    private final PersonalFinanceService personalFinanceService;

    @Scheduled(cron = "${mana.personal-finance.scheduler.cron:0 0 1 * * ?}")
    public void processDueRecurringTransactions() {
        log.info("Starting Personal Finance Recurring Transaction background processing...");
        try {
            int count = personalFinanceService.processAllDueRecurring();
            log.info("Finished processing {} due recurring personal transactions", count);
        } catch (Exception ex) {
            log.error("Failed to complete recurring transaction background run: {}", ex.getMessage(), ex);
        }
    }
}
