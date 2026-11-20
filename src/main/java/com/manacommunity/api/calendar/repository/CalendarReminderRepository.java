package com.manacommunity.api.calendar.repository;

import com.manacommunity.api.calendar.entity.CalendarReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface CalendarReminderRepository extends JpaRepository<CalendarReminder, Long> {

    List<CalendarReminder> findByUserIdAndEventSourceAndSourceId(Long userId, String eventSource, String sourceId);

    List<CalendarReminder> findByRemindAtBeforeAndSentFalse(Instant deadline);

    boolean existsByUserIdAndEventSourceAndSourceId(Long userId, String eventSource, String sourceId);

    void deleteByUserIdAndEventSourceAndSourceId(Long userId, String eventSource, String sourceId);
}
