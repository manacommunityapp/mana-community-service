package com.manacommunity.api.unit.access.biometric;

import com.manacommunity.api.access.biometric.*;
import com.manacommunity.api.access.biometric.BiometricEnums.*;
import com.manacommunity.api.access.biometric.engine.TurnstileAccessRuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TurnstileAccessRuleEngine Unit Tests")
public class TurnstileAccessRuleEngineTest {

    private TurnstileAccessRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new TurnstileAccessRuleEngine();
    }

    @Test
    @DisplayName("Resident with high confidence match is granted open access")
    void testResidentAccessGranted() {
        BiometricTurnstile turnstile = BiometricTurnstile.builder()
                .turnstileIdentifier("TS-01")
                .status(TurnstileStatus.ONLINE)
                .build();

        BiometricUserEnrollment enrollment = BiometricUserEnrollment.builder()
                .personType(BiometricPersonType.RESIDENT)
                .enrollmentStatus(EnrollmentStatus.ENROLLED)
                .build();

        var result = engine.evaluate(turnstile, Optional.of(enrollment), new BigDecimal("0.96"), LocalDateTime.now());

        assertEquals(AccessDecision.GRANTED_OPEN, result.decision());
        assertTrue(result.unlockRelay());
    }

    @Test
    @DisplayName("Domestic staff outside allowed entry window is denied")
    void testStaffOutsideHoursDenied() {
        BiometricTurnstile turnstile = BiometricTurnstile.builder()
                .turnstileIdentifier("TS-01")
                .status(TurnstileStatus.ONLINE)
                .build();

        BiometricUserEnrollment staff = BiometricUserEnrollment.builder()
                .personType(BiometricPersonType.DOMESTIC_STAFF)
                .enrollmentStatus(EnrollmentStatus.ENROLLED)
                .timeWindowStart("06:00")
                .timeWindowEnd("18:00")
                .allowedDays("MON,TUE,WED,THU,FRI,SAT")
                .build();

        // 21:30 PM (after 18:00)
        LocalDateTime lateNight = LocalDateTime.of(2026, 10, 2, 21, 30);

        var result = engine.evaluate(turnstile, Optional.of(staff), new BigDecimal("0.94"), lateNight);

        assertEquals(AccessDecision.DENIED_OUTSIDE_HOURS, result.decision());
        assertFalse(result.unlockRelay());
    }

    @Test
    @DisplayName("Turnstile in LOCKDOWN blocks ordinary residents but allows security guards")
    void testLockdownModeRules() {
        BiometricTurnstile turnstile = BiometricTurnstile.builder()
                .turnstileIdentifier("TS-01")
                .status(TurnstileStatus.LOCKDOWN)
                .build();

        BiometricUserEnrollment resident = BiometricUserEnrollment.builder()
                .personType(BiometricPersonType.RESIDENT)
                .enrollmentStatus(EnrollmentStatus.ENROLLED)
                .build();

        BiometricUserEnrollment guard = BiometricUserEnrollment.builder()
                .personType(BiometricPersonType.SECURITY_GUARD)
                .enrollmentStatus(EnrollmentStatus.ENROLLED)
                .build();

        var resResult = engine.evaluate(turnstile, Optional.of(resident), new BigDecimal("0.95"), LocalDateTime.now());
        assertEquals(AccessDecision.DENIED_LOCKDOWN, resResult.decision());
        assertFalse(resResult.unlockRelay());

        var guardResult = engine.evaluate(turnstile, Optional.of(guard), new BigDecimal("0.95"), LocalDateTime.now());
        assertEquals(AccessDecision.GRANTED_OPEN, guardResult.decision());
        assertTrue(guardResult.unlockRelay());
    }
}
