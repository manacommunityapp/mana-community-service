package com.manacommunity.api.unit.util;

import com.manacommunity.api.util.IndianDateTimeUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IndianDateTimeUtils Unit Tests")
class IndianDateTimeUtilsTest {

    @Test
    @DisplayName("formatTime12Hour should correctly format 24h times to Indian 12h AM/PM")
    void testFormatTime12Hour() {
        assertThat(IndianDateTimeUtils.formatTime12Hour("09:00")).isEqualTo("09:00 AM");
        assertThat(IndianDateTimeUtils.formatTime12Hour("18:30")).isEqualTo("06:30 PM");
        assertThat(IndianDateTimeUtils.formatTime12Hour("00:00")).isEqualTo("12:00 AM");
        assertThat(IndianDateTimeUtils.formatTime12Hour("12:00")).isEqualTo("12:00 PM");
        assertThat(IndianDateTimeUtils.formatTime12Hour("12:30")).isEqualTo("12:30 PM");
        assertThat(IndianDateTimeUtils.formatTime12Hour("23:59")).isEqualTo("11:59 PM");
    }

    @Test
    @DisplayName("formatIndianDate should format to dd/MM/yyyy")
    void testFormatIndianDate() {
        LocalDate date = LocalDate.of(2026, 8, 29);
        assertThat(IndianDateTimeUtils.formatIndianDate(date)).isEqualTo("29/08/2026");
    }

    @Test
    @DisplayName("formatIndianDateTime should format to dd/MM/yyyy, hh:mm a")
    void testFormatIndianDateTime() {
        LocalDateTime dt = LocalDateTime.of(2026, 8, 29, 18, 30);
        assertThat(IndianDateTimeUtils.formatIndianDateTime(dt)).isEqualTo("29/08/2026, 06:30 PM");
    }

    @Test
    @DisplayName("isTimeRangeValid should return true only when end time is after start time")
    void testIsTimeRangeValid() {
        assertThat(IndianDateTimeUtils.isTimeRangeValid("09:00", "10:30")).isTrue();
        assertThat(IndianDateTimeUtils.isTimeRangeValid("18:00", "17:00")).isFalse();
        assertThat(IndianDateTimeUtils.isTimeRangeValid("09:00", "09:00")).isFalse();
    }

    @Test
    @DisplayName("isSlotInPastIST should accurately evaluate past and future slots")
    void testIsSlotInPastIST() {
        LocalDate past = LocalDate.of(2020, 1, 1);
        LocalDate future = LocalDate.of(2099, 1, 1);

        assertThat(IndianDateTimeUtils.isSlotInPastIST(past, LocalTime.of(10, 0))).isTrue();
        assertThat(IndianDateTimeUtils.isSlotInPastIST(future, LocalTime.of(10, 0))).isFalse();
    }
}
