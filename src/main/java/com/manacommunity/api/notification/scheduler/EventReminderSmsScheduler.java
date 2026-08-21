package com.manacommunity.api.notification.scheduler;

import com.manacommunity.api.notification.event.EventReminderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Publishes EventReminderEvent for events starting in ~24h and ~1h.
 * The actual SMS delivery is handled by EventCancellationSmsHandler.
 * Requires an EventRepository (or similar) to query upcoming events;
 * this is a stub — wire in the real event query once the Event module is stable.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventReminderSmsScheduler {

    private final ApplicationEventPublisher eventPublisher;

    /** Runs every 30 minutes; finds events starting 23h50m–24h10m from now. */
    @Scheduled(fixedDelayString = "1800000")
    public void send24hReminders() {
        // TODO: query EventRepository for events in [now+23h50m, now+24h10m]
        // and publish EventReminderEvent(reminderType="24h") for each attendee
        log.debug("24h event reminder scheduler tick");
    }

    /** Runs every 10 minutes; finds events starting 50m–70m from now. */
    @Scheduled(fixedDelayString = "600000")
    public void send1hReminders() {
        // TODO: query EventRepository for events in [now+50m, now+70m]
        // and publish EventReminderEvent(reminderType="1h") for each attendee
        log.debug("1h event reminder scheduler tick");
    }
}
