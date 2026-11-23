package com.manacommunity.api.freshkitchen.scheduler;

import com.manacommunity.api.common.lock.DistributedJobLockService;
import com.manacommunity.api.freshkitchen.service.DailyBasketService;
import com.manacommunity.api.freshkitchen.service.FreshKitchenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyBasketScheduler {

    private final DailyBasketService dailyBasketService;
    private final FreshKitchenService freshKitchenService;
    private final DistributedJobLockService jobLockService;

    /**
     * Nightly 10:00 PM IST batch job:
     * Evaluates all resident daily baskets and generates authoritative CommerceOrders
     * for tomorrow morning's fresh batter and daily essentials delivery.
     * Guarded with distributed lock so only one cluster node executes order generation.
     */
    @Scheduled(cron = "0 0 22 * * *", zone = "Asia/Kolkata")
    public void runNightlyDailyBasketGeneration() {
        jobLockService.executeWithLock("daily_basket_nightly_generation", Duration.ofMinutes(30), () -> {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            log.info("Starting Nightly Daily Basket CommerceOrder generation for {}", tomorrow);
            try {
                int ordersCreated = dailyBasketService.generateDailyOrders(tomorrow);
                log.info("Nightly Daily Basket run complete for {}. Generated {} orders.", tomorrow, ordersCreated);
            } catch (Exception e) {
                log.error("Error during nightly daily basket order generation for {}: {}", tomorrow, e.getMessage(), e);
            }
        });
    }

    /**
     * Evening 8:00 PM IST reminder job:
     * Sends push notification reminders to residents scheduled for tomorrow morning's delivery
     * informing them to review, customize, or pause their basket before the 10:00 PM cutoff.
     */
    @Scheduled(cron = "0 0 20 * * *", zone = "Asia/Kolkata")
    public void runEveningCutoffReminders() {
        jobLockService.executeWithLock("daily_basket_cutoff_reminders", Duration.ofMinutes(15), () -> {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            log.info("Starting evening 8:00 PM Daily Basket cutoff reminders for {}", tomorrow);
            try {
                dailyBasketService.sendNightlyCutoffReminders(tomorrow);
            } catch (Exception e) {
                log.error("Error during evening cutoff reminders: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Morning 6:00 AM IST dispatch notification job:
     * Transitions today's confirmed morning orders to OUT_FOR_DELIVERY and sends
     * push notifications containing the live 6-digit doorstep handover PIN.
     */
    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Kolkata")
    public void runMorningDispatchAlerts() {
        jobLockService.executeWithLock("daily_basket_morning_dispatch", Duration.ofMinutes(20), () -> {
            LocalDate today = LocalDate.now();
            log.info("Starting morning 6:00 AM Daily Basket dispatch notifications for {}", today);
            try {
                int dispatched = dailyBasketService.dispatchMorningOrders(today);
                log.info("Morning dispatch completed for {}. Notified {} residents.", today, dispatched);
            } catch (Exception e) {
                log.error("Error during morning dispatch alerts: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Quality Gate & Expiry Automation:
     * Runs every 5 minutes to verify batches of perishable batters, fresh cuts, and meal kits.
     * Automatically transitions any batch past its use-by timestamp to EXPIRED.
     */
    @Scheduled(fixedDelay = 300_000)
    public void runAutomatedBatchExpiryCheck() {
        jobLockService.executeWithLock("fresh_kitchen_batch_expiry_check", Duration.ofMinutes(4), () -> {
            try {
                int expiredCount = freshKitchenService.expireOverdueBatches();
                if (expiredCount > 0) {
                    log.info("Batch expiry check completed. Locked out {} overdue batches.", expiredCount);
                }
            } catch (Exception e) {
                log.error("Error checking batch expiry: {}", e.getMessage(), e);
            }
        });
    }
}

