package com.manacommunity.api.freshkitchen.scheduler;

import com.manacommunity.api.freshkitchen.service.DailyBasketService;
import com.manacommunity.api.freshkitchen.service.FreshKitchenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyBasketScheduler {

    private final DailyBasketService dailyBasketService;
    private final FreshKitchenService freshKitchenService;

    /**
     * Nightly 10:00 PM IST batch job:
     * Evaluates all resident daily baskets and generates authoritative CommerceOrders
     * for tomorrow morning's fresh batter and daily essentials delivery.
     */
    @Scheduled(cron = "0 0 22 * * *")
    public void runNightlyDailyBasketGeneration() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        log.info("Starting Nightly Daily Basket CommerceOrder generation for {}", tomorrow);
        try {
            int ordersCreated = dailyBasketService.generateDailyOrders(tomorrow);
            log.info("Nightly Daily Basket run complete for {}. Generated {} orders.", tomorrow, ordersCreated);
        } catch (Exception e) {
            log.error("Error during nightly daily basket order generation for {}: {}", tomorrow, e.getMessage(), e);
        }
    }

    /**
     * Quality Gate & Expiry Automation:
     * Runs every 5 minutes to verify batches of perishable batters, fresh cuts, and meal kits.
     * Automatically transitions any batch past its use-by timestamp to EXPIRED.
     */
    @Scheduled(fixedDelay = 300_000)
    public void runAutomatedBatchExpiryCheck() {
        try {
            int expiredCount = freshKitchenService.expireOverdueBatches();
            if (expiredCount > 0) {
                log.info("Batch expiry check completed. Locked out {} overdue batches.", expiredCount);
            }
        } catch (Exception e) {
            log.error("Error checking batch expiry: {}", e.getMessage(), e);
        }
    }
}
