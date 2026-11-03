package com.manacommunity.api.wellnessbasket.scheduler;

import com.manacommunity.api.wellnessbasket.service.WellnessSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "mana.wellness.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class WellnessScheduler {

    private final WellnessSubscriptionService subscriptionService;
    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    /**
     * 10:00 PM Nightly:
     * Generates wellness orders for tomorrow's deliveries (Mon-Sat).
     * Integrates with the DailyBasket and CommerceOrder engine.
     */
    @Scheduled(cron = "0 0 22 * * *", zone = "Asia/Kolkata")
    public void generateNightlyWellnessOrders() {
        LocalDate tomorrow = LocalDate.now(IST_ZONE).plusDays(1);
        log.info("Running nightly Wellness Subscription order generation for {}", tomorrow);
        try {
            int count = subscriptionService.generateDailyWellnessBaskets(tomorrow);
            log.info("Completed nightly Wellness order run for {}. Generated {} orders.", tomorrow, count);
        } catch (Exception e) {
            log.error("Error generating nightly Wellness orders for {}: {}", tomorrow, e.getMessage(), e);
        }
    }

    /**
     * Friday 6:00 PM IST:
     * Notification event that weekly swap window is now OPEN for next week's menu.
     */
    @Scheduled(cron = "0 0 18 * * FRI", zone = "Asia/Kolkata")
    public void notifySwapWindowOpen() {
        log.info("Weekly Wellness Swap Window is now OPEN. Residents can customize next week's menu until Saturday 8:00 PM IST.");
    }

    /**
     * Saturday 8:00 PM IST:
     * Notification event that weekly swap window is now CLOSED.
     */
    @Scheduled(cron = "0 0 20 * * SAT", zone = "Asia/Kolkata")
    public void notifySwapWindowClosed() {
        log.info("Weekly Wellness Swap Window is now CLOSED. Production manifests are locked for dietitian & vendor scheduling.");
    }
}
