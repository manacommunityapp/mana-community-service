package com.manacommunity.api.unit.notification.orchestrator;

import com.manacommunity.api.notification.orchestrator.NotificationEnums.*;
import com.manacommunity.api.notification.orchestrator.NotificationPreference;
import com.manacommunity.api.notification.orchestrator.engine.NotificationPreferenceEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NotificationPreferenceEngine Unit Tests")
public class NotificationPreferenceEngineTest {

    private NotificationPreferenceEngine engine;

    @BeforeEach
    void setUp() {
        engine = new NotificationPreferenceEngine();
    }

    @Test
    @DisplayName("Critical emergency notification should bypass quiet hours and opt-outs")
    void testCriticalEmergencyBypass() {
        NotificationPreference optOutPref = NotificationPreference.builder()
                .isEnabled(false)
                .quietHoursEnabled(true)
                .quietHoursStart("22:00")
                .quietHoursEnd("07:00")
                .build();

        var decision = engine.evaluate(
                NotificationCategory.EMERGENCY_SOS,
                NotificationChannel.SMS,
                NotificationPriority.CRITICAL,
                Optional.of(optOutPref),
                LocalTime.of(23, 30)
        );

        assertTrue(decision.allowed());
        assertEquals(DeliveryStatus.QUEUED, decision.status());
    }

    @Test
    @DisplayName("Quiet hours should suppress normal push notification during night window")
    void testQuietHoursSuppression() {
        NotificationPreference pref = NotificationPreference.builder()
                .isEnabled(true)
                .quietHoursEnabled(true)
                .quietHoursStart("22:00")
                .quietHoursEnd("07:00")
                .build();

        var decision = engine.evaluate(
                NotificationCategory.COMMUNITY_NOTICE,
                NotificationChannel.PUSH,
                NotificationPriority.NORMAL,
                Optional.of(pref),
                LocalTime.of(23, 15) // Inside 22:00 -> 07:00
        );

        assertFalse(decision.allowed());
        assertEquals(DeliveryStatus.SUPPRESSED_QUIET_HOURS, decision.status());
    }

    @Test
    @DisplayName("In-app notification should never be suppressed by quiet hours")
    void testInAppAllowedDuringQuietHours() {
        NotificationPreference pref = NotificationPreference.builder()
                .isEnabled(true)
                .quietHoursEnabled(true)
                .quietHoursStart("22:00")
                .quietHoursEnd("07:00")
                .build();

        var decision = engine.evaluate(
                NotificationCategory.COMMUNITY_NOTICE,
                NotificationChannel.IN_APP,
                NotificationPriority.NORMAL,
                Optional.of(pref),
                LocalTime.of(23, 15)
        );

        assertTrue(decision.allowed());
    }

    @Test
    @DisplayName("Explicit user opt-out should block notification")
    void testExplicitOptOut() {
        NotificationPreference pref = NotificationPreference.builder()
                .isEnabled(false)
                .build();

        var decision = engine.evaluate(
                NotificationCategory.FINANCIAL_BILLING,
                NotificationChannel.SMS,
                NotificationPriority.NORMAL,
                Optional.of(pref),
                LocalTime.of(12, 0)
        );

        assertFalse(decision.allowed());
        assertEquals(DeliveryStatus.OPTED_OUT, decision.status());
    }

    @Test
    @DisplayName("Overnight quiet hours boundary evaluation")
    void testOvernightQuietHoursWindow() {
        assertTrue(engine.isInsideQuietHours(LocalTime.of(22, 0), "22:00", "07:00"));
        assertTrue(engine.isInsideQuietHours(LocalTime.of(3, 30), "22:00", "07:00"));
        assertTrue(engine.isInsideQuietHours(LocalTime.of(6, 59), "22:00", "07:00"));
        assertFalse(engine.isInsideQuietHours(LocalTime.of(7, 0), "22:00", "07:00"));
        assertFalse(engine.isInsideQuietHours(LocalTime.of(14, 0), "22:00", "07:00"));
    }
}
