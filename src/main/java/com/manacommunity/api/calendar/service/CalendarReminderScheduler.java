package com.manacommunity.api.calendar.service;

import com.manacommunity.api.calendar.entity.CalendarReminder;
import com.manacommunity.api.calendar.repository.CalendarReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CalendarReminderScheduler {

    private final CalendarReminderRepository repo;

    @Scheduled(fixedDelay = 60_000)
    public void processReminders() {
        Instant now = Instant.now();
        List<CalendarReminder> due = repo.findByRemindAtBeforeAndSentFalse(now);
        for (CalendarReminder r : due) {
            try {
                log.info("[CalReminder] user={} source={} id={} remind={}min",
                        r.getUserId(), r.getEventSource(), r.getSourceId(), r.getRemindMinutesBefore());
                r.setSent(true);
                repo.save(r);
            } catch (Exception e) {
                log.error("[CalReminder] failed for id={}", r.getId(), e);
            }
        }
    }
}
