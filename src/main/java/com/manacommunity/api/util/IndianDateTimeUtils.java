package com.manacommunity.api.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Utility class for Indian Standard Time (IST, UTC+05:30) date & time operations,
 * 12-hour AM/PM formatting, and time validations across the backend.
 */
public final class IndianDateTimeUtils {

    public static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    public static final DateTimeFormatter TIME_12H_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    public static final DateTimeFormatter DATE_INDIAN_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
    public static final DateTimeFormatter DATE_TIME_INDIAN_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy, hh:mm a", Locale.ENGLISH);
    public static final DateTimeFormatter TIME_24H_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    private IndianDateTimeUtils() {
        // Prevent instantiation
    }

    /**
     * Returns current LocalDateTime in IST.
     */
    public static LocalDateTime nowInIST() {
        return LocalDateTime.now(IST_ZONE);
    }

    /**
     * Returns current LocalDate in IST.
     */
    public static LocalDate todayInIST() {
        return LocalDate.now(IST_ZONE);
    }

    /**
     * Returns current LocalTime in IST.
     */
    public static LocalTime currentTimeInIST() {
        return LocalTime.now(IST_ZONE);
    }

    /**
     * Formats LocalTime into Indian 12-hour format with AM/PM (e.g. "09:30 AM", "06:15 PM").
     */
    public static String formatTime12Hour(LocalTime time) {
        if (time == null) return "";
        return time.format(TIME_12H_FORMATTER).toUpperCase(Locale.ENGLISH);
    }

    /**
     * Formats a raw time string (e.g. "09:00", "18:30:00", "9:00") into Indian 12-hour format.
     */
    public static String formatTime12Hour(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return "";
        LocalTime parsed = parseTime(timeStr);
        return parsed != null ? formatTime12Hour(parsed) : timeStr.trim();
    }

    /**
     * Formats LocalDate into Indian date format "dd/MM/yyyy" (e.g. "29/08/2026").
     */
    public static String formatIndianDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_INDIAN_FORMATTER);
    }

    /**
     * Formats LocalDateTime into Indian date & time format (e.g. "29/08/2026, 06:30 PM").
     */
    public static String formatIndianDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DATE_TIME_INDIAN_FORMATTER).toUpperCase(Locale.ENGLISH);
    }

    /**
     * Flexible parser for time strings in 24-hour ("18:30", "09:00:00") or 12-hour ("6:30 PM", "09:00 AM") format.
     */
    public static LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return null;
        String clean = timeStr.trim();

        if (clean.contains("T")) {
            clean = clean.substring(clean.indexOf('T') + 1);
        }

        // Try standard 24-hour HH:mm
        try {
            if (clean.length() == 5 && clean.charAt(2) == ':') {
                return LocalTime.parse(clean, TIME_24H_FORMATTER);
            }
        } catch (DateTimeParseException ignored) {}

        // Try standard ISO-8601 LocalTime (e.g. "18:30:00" or "09:00")
        try {
            return LocalTime.parse(clean);
        } catch (DateTimeParseException ignored) {}

        // Try 12-hour format "hh:mm a"
        try {
            return LocalTime.parse(clean.toUpperCase(Locale.ENGLISH), TIME_12H_FORMATTER);
        } catch (DateTimeParseException ignored) {}

        // Regex fallback
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,2})[:.](\\d{2})(?::(\\d{2}))?\\s*(am|pm)?", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(clean);
        if (m.find()) {
            int h = Integer.parseInt(m.group(1));
            int min = Integer.parseInt(m.group(2));
            int sec = m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;
            String meridian = m.group(4);

            if (meridian != null) {
                if ("pm".equalsIgnoreCase(meridian) && h < 12) h += 12;
                if ("am".equalsIgnoreCase(meridian) && h == 12) h = 0;
            }
            if (h >= 0 && h < 24 && min >= 0 && min < 60) {
                return LocalTime.of(h, min, sec);
            }
        }

        return null;
    }

    /**
     * Validates that end time is strictly after start time.
     */
    public static boolean isTimeRangeValid(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) return false;
        return endTime.isAfter(startTime);
    }

    /**
     * Validates that end time is strictly after start time from raw strings.
     */
    public static boolean isTimeRangeValid(String startTimeStr, String endTimeStr) {
        LocalTime start = parseTime(startTimeStr);
        LocalTime end = parseTime(endTimeStr);
        return isTimeRangeValid(start, end);
    }

    /**
     * Checks if a given slot date and time is in the past in IST.
     */
    public static boolean isSlotInPastIST(LocalDate slotDate, LocalTime slotTime) {
        if (slotDate == null) return false;
        LocalDate today = todayInIST();
        if (slotDate.isBefore(today)) return true;
        if (slotDate.isEqual(today)) {
            if (slotTime == null) return false;
            return slotTime.isBefore(currentTimeInIST());
        }
        return false;
    }
}
