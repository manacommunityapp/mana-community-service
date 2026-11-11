package com.manacommunity.api.sports.scheduler;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Owns time-slot allocation for generated schedules: where the day starts and
 * how successive matches are spaced (duration + break). Extracted from the
 * inline cursors that used to live in every generator.
 */
@Slf4j
@Service
public class SportsTimeSlotAllocator {

    private static final DateTimeFormatter[] TIME_FORMATTERS = new DateTimeFormatter[] {
        DateTimeFormatter.ISO_LOCAL_TIME,
        DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("hh:mma", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("h:mm:ss a", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("H:mm", Locale.ENGLISH)
    };

    /** Default start-of-play time for a generated day (09:00). */
    public LocalDateTime dayStart(LocalDate date) {
        return date.atTime(9, 0);
    }

    /** A sequential slot cursor spaced by (durationMins + breakMins). */
    public Cursor cursor(LocalDate startDate, int durationMins, int breakMins) {
        return new Cursor(dayStart(startDate), durationMins + breakMins);
    }

    /** A sequential slot cursor from an explicit start time. */
    public Cursor cursor(LocalDateTime start, int durationMins, int breakMins) {
        return new Cursor(start, durationMins + breakMins);
    }

    /** Parse a UI-supplied date/time ("yyyy-MM-dd" + "HH:mm" or "hh:mm a") into a timestamp. */
    public LocalDateTime parseScheduledAt(String date, String time) {
        try {
            if (date == null || date.isBlank()) {
                return LocalDateTime.now();
            }
            String trimmedDate = date.trim();
            if (trimmedDate.contains("T")) {
                return LocalDateTime.parse(trimmedDate);
            }
            LocalDate d = LocalDate.parse(trimmedDate);
            LocalTime t = parseLocalTime(time);
            return LocalDateTime.of(d, t);
        } catch (Exception e) {
            log.warn("Failed to parse scheduledAt date={} time={}, falling back to now", date, time, e);
            return LocalDateTime.now();
        }
    }

    public LocalTime parseLocalTime(String time) {
        if (time == null || time.isBlank()) {
            return LocalTime.MIDNIGHT;
        }
        String t = time.trim();
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalTime.parse(t, formatter);
            } catch (Exception ignored) {
            }
        }
        String upper = t.toUpperCase(Locale.ENGLISH);
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalTime.parse(upper, formatter);
            } catch (Exception ignored) {
            }
        }
        log.warn("Could not parse time string '{}', defaulting to midnight", time);
        return LocalTime.MIDNIGHT;
    }

    /**
     * Mutable cursor that yields successive, evenly-spaced match start times.
     * {@link #next()} returns the current slot and advances; {@link #peek()}
     * and {@link #set(LocalDateTime)} allow callers that need to jump.
     */
    public static final class Cursor {
        private LocalDateTime current;
        private final int stepMinutes;

        Cursor(LocalDateTime start, int stepMinutes) {
            this.current = start;
            this.stepMinutes = stepMinutes;
        }

        public LocalDateTime next() {
            LocalDateTime slot = current;
            current = current.plusMinutes(stepMinutes);
            return slot;
        }

        public LocalDateTime peek() {
            return current;
        }

        public void set(LocalDateTime at) {
            this.current = at;
        }
    }
}
