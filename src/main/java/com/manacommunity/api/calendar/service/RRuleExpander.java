package com.manacommunity.api.calendar.service;

import com.manacommunity.api.calendar.entity.PersonalCalendarEvent;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.*;

@Component
public class RRuleExpander {

    public List<Instant[]> expand(PersonalCalendarEvent event, Instant from, Instant to) {
        if (event.getRrule() == null || event.getRrule().isBlank()) {
            List<Instant[]> single = new ArrayList<>();
            single.add(new Instant[]{event.getStartTime(), event.getEndTime()});
            return single;
        }

        Map<String, String> parts = parseRRule(event.getRrule());
        String freq = parts.getOrDefault("FREQ", "DAILY");
        int interval = Integer.parseInt(parts.getOrDefault("INTERVAL", "1"));
        int count = parts.containsKey("COUNT") ? Integer.parseInt(parts.get("COUNT")) : Integer.MAX_VALUE;
        Instant until = parts.containsKey("UNTIL") ? parseUntil(parts.get("UNTIL")) : to;
        Instant effectiveEnd = until.isBefore(to) ? until : to;

        Duration duration = Duration.between(event.getStartTime(), event.getEndTime());
        List<Instant[]> results = new ArrayList<>();
        Instant cursor = event.getStartTime();
        int occurrences = 0;

        while (!cursor.isAfter(effectiveEnd) && occurrences < count) {
            if (!cursor.isBefore(from)) {
                results.add(new Instant[]{cursor, cursor.plus(duration)});
            }
            cursor = advance(cursor, freq, interval);
            occurrences++;
            if (results.size() > 500) break;
        }
        return results;
    }

    private Instant advance(Instant t, String freq, int interval) {
        ZonedDateTime zdt = t.atZone(ZoneOffset.UTC);
        return switch (freq) {
            case "WEEKLY"  -> zdt.plusWeeks(interval).toInstant();
            case "MONTHLY" -> zdt.plusMonths(interval).toInstant();
            case "YEARLY"  -> zdt.plusYears(interval).toInstant();
            default        -> zdt.plusDays(interval).toInstant();
        };
    }

    private Map<String, String> parseRRule(String rrule) {
        Map<String, String> map = new HashMap<>();
        for (String part : rrule.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) map.put(kv[0].trim().toUpperCase(), kv[1].trim());
        }
        return map;
    }

    private Instant parseUntil(String until) {
        try {
            String s = until.replace("T", "").replace("Z", "");
            int year  = Integer.parseInt(s.substring(0, 4));
            int month = Integer.parseInt(s.substring(4, 6));
            int day   = Integer.parseInt(s.substring(6, 8));
            return LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (Exception e) {
            return Instant.now().plus(Duration.ofDays(365 * 5));
        }
    }
}
