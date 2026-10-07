package com.manacommunity.api.sports.scheduler.engine.constraint;

import com.manacommunity.api.sports.scheduler.engine.dto.ScheduleConstraintOptions;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class TimeSlotAllocationConstraint {

    public LocalDateTime calculateNextSlot(LocalDateTime current, LocalDate startDate,
                                           ScheduleConstraintOptions options) {
        if (current == null) {
            return startDate.atTime(options.getDayStartTime());
        }

        LocalDateTime next = current.plusMinutes(options.getMatchDurationMinutes() + options.getChangeoverBufferMinutes());
        if (next.toLocalTime().isAfter(options.getDayEndTime().minusMinutes(options.getMatchDurationMinutes()))) {
            // Roll over to next morning
            return next.toLocalDate().plusDays(1).atTime(options.getDayStartTime());
        }
        return next;
    }
}
