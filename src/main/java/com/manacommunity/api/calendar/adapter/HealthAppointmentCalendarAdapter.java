package com.manacommunity.api.calendar.adapter;

import com.manacommunity.api.calendar.CalendarType;
import com.manacommunity.api.calendar.EventSource;
import com.manacommunity.api.calendar.dto.CalendarEventDto;
import com.manacommunity.api.health.repository.HealthAppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HealthAppointmentCalendarAdapter implements CalendarAdapter {

    private final HealthAppointmentRepository appointmentRepository;

    @Override public EventSource getSource() { return EventSource.HEALTH_APPOINTMENT; }
    @Override public CalendarType getCalendarType() { return CalendarType.APPOINTMENT_CALENDAR; }

    @Override
    public List<CalendarEventDto> fetchEvents(Long communityId, Long userId, Instant from, Instant to) {
        return appointmentRepository
                .findByTenantIdAndBookedByUserIdOrderBySlotDateDescSlotStartTimeDesc(communityId, userId)
                .stream()
                .filter(a -> {
                    Instant start = a.getSlotDate().atTime(a.getSlotStartTime()).toInstant(ZoneOffset.UTC);
                    return !start.isBefore(from) && start.isBefore(to);
                })
                .map(a -> {
                    Instant start = a.getSlotDate().atTime(a.getSlotStartTime()).toInstant(ZoneOffset.UTC);
                    Instant end = a.getSlotDate().atTime(a.getSlotEndTime()).toInstant(ZoneOffset.UTC);
                    String doctorLabel = a.getDoctor() != null ? "Dr. " + a.getDoctor().getProviderId() : "Doctor";
                    boolean cancelled = "CANCELLED".equalsIgnoreCase(a.getStatus() != null ? a.getStatus().name() : "");
                    return new CalendarEventDto(
                            "HEALTH_APPOINTMENT:" + a.getId(),
                            "Appointment: " + doctorLabel,
                            a.getSymptomsSummary(),
                            null, start, end, false, "UTC",
                            CalendarType.APPOINTMENT_CALENDAR, EventSource.HEALTH_APPOINTMENT,
                            String.valueOf(a.getId()), "#0891B2", cancelled, false,
                            "HEALTH_APPOINTMENT", String.valueOf(a.getId()), null
                    );
                })
                .toList();
    }
}
