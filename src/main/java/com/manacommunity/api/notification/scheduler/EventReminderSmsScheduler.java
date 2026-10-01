package com.manacommunity.api.notification.scheduler;

import com.manacommunity.api.events.entity.EventBookingRegistration;
import com.manacommunity.api.events.entity.EventCommunity;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.notification.event.EventReminderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Publishes {@link EventReminderEvent} for events starting in ~24 h and ~1 h.
 *
 * <p>The scheduler queries all PUBLISHED / ACTIVE events whose start timestamp
 * falls within a configurable window, then fetches every confirmed
 * (non-cancelled) attendee who has a phone number and fires an
 * {@link EventReminderEvent} per attendee.  The actual SMS delivery is
 * handled downstream by the {@code EventCancellationSmsHandler} listener.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventReminderSmsScheduler {

    private final ApplicationEventPublisher eventPublisher;
    private final EventCommunityRepository  eventCommunityRepository;
    private final EventBookingRegistrationRepository registrationRepository;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ── 24-hour reminder ────────────────────────────────────────────────────

    /**
     * Runs every 30 minutes; finds events starting 23 h 50 m – 24 h 10 m from now
     * and notifies all confirmed attendees.
     */
    @Scheduled(fixedDelayString = "1800000")
    public void send24hReminders() {
        LocalDateTime now  = LocalDateTime.now();
        LocalDateTime from = now.plusMinutes(23 * 60 + 50);   // +23h50m
        LocalDateTime to   = now.plusMinutes(24 * 60 + 10);   // +24h10m
        sendRemindersForWindow(from, to, "24h");
    }

    // ── 1-hour reminder ─────────────────────────────────────────────────────

    /**
     * Runs every 10 minutes; finds events starting 50 m – 70 m from now
     * and notifies all confirmed attendees.
     */
    @Scheduled(fixedDelayString = "600000")
    public void send1hReminders() {
        LocalDateTime now  = LocalDateTime.now();
        LocalDateTime from = now.plusMinutes(50);
        LocalDateTime to   = now.plusMinutes(70);
        sendRemindersForWindow(from, to, "1h");
    }

    // ── shared helper ────────────────────────────────────────────────────────

    private void sendRemindersForWindow(LocalDateTime from, LocalDateTime to, String reminderType) {
        List<EventCommunity> events = eventCommunityRepository.findEventsStartingBetween(from, to);
        if (events.isEmpty()) {
            log.debug("[EventReminderSms] No events in {} window [{} – {}]", reminderType, from, to);
            return;
        }

        log.info("[EventReminderSms] {} window: {} event(s) to remind", reminderType, events.size());

        for (EventCommunity event : events) {
            String eventTime    = buildDisplayTime(event);
            String venueOrLink  = resolveVenueOrLink(event);

            List<EventBookingRegistration> registrations =
                    registrationRepository.findByMainEventIdOrderByCreatedAtDesc(event.getId());

            int sent = 0;
            for (EventBookingRegistration reg : registrations) {
                if ("CANCELLED".equalsIgnoreCase(reg.getStatus())) continue;
                if (reg.getUser() == null)                          continue;

                String phone = reg.getUser().getPhone();
                if (phone == null || phone.isBlank())               continue;

                eventPublisher.publishEvent(new EventReminderEvent(
                        reg.getUser().getId(),
                        phone,
                        event.getTitle(),
                        eventTime,
                        venueOrLink,
                        reminderType
                ));
                sent++;
            }

            log.info("[EventReminderSms] eventId={} '{}' → {} reminder(s) queued ({})",
                    event.getId(), event.getTitle(), sent, reminderType);
        }
    }

    /** Formats the event start date + time for display in the SMS body. */
    private String buildDisplayTime(EventCommunity event) {
        LocalDateTime start = event.getStartDate().atTime(
                event.getStartTime() != null ? event.getStartTime()
                                             : java.time.LocalTime.of(0, 0)
        );
        return start.format(DISPLAY_FMT);
    }

    /**
     * Returns the venue string for in-person events or the {@code location}
     * field (which holds the meeting URL) for online events.
     */
    private String resolveVenueOrLink(EventCommunity event) {
        if (event.getVenue() != null && !event.getVenue().isBlank()) {
            return event.getVenue();
        }
        if (event.getLocation() != null && !event.getLocation().isBlank()) {
            return event.getLocation();
        }
        return "venue TBD";
    }
}
