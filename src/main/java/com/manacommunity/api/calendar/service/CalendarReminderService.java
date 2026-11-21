package com.manacommunity.api.calendar.service;

import com.manacommunity.api.calendar.entity.CalendarReminder;
import com.manacommunity.api.calendar.repository.CalendarReminderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CalendarReminderService {

    private final CalendarReminderRepository repo;

    @Transactional
    public void setReminder(Long userId, String eventSource, String sourceId,
            int minutesBefore, Instant eventStartTime) {
        CalendarReminder reminder = CalendarReminder.builder()
                .userId(userId)
                .eventSource(eventSource)
                .sourceId(sourceId)
                .remindMinutesBefore(minutesBefore)
                .remindAt(eventStartTime.minus(Duration.ofMinutes(minutesBefore)))
                .sent(false)
                .build();
        repo.save(reminder);
    }

    @Transactional
    public void removeReminder(Long userId, String eventSource, String sourceId) {
        repo.deleteByUserIdAndEventSourceAndSourceId(userId, eventSource, sourceId);
    }
}
