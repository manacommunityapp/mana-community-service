package com.manacommunity.api.parking.unit;

import com.manacommunity.api.parking.engine.ParkingViolationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Parking Violation Engine Unit Tests")
class ParkingViolationEngineTest {

    private ParkingViolationEngine violationEngine;

    @BeforeEach
    void setUp() {
        violationEngine = new ParkingViolationEngine();
    }

    @Test
    @DisplayName("Should return no violation when visitor pass is active")
    void shouldReturnNoViolationWhenPassActive() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime validUntil = now.plusHours(2);

        var assessment = violationEngine.assessVisitorPass(validUntil, now, null);
        assertFalse(assessment.isViolation());
        assertEquals(ParkingViolationEngine.ViolationSeverity.NONE, assessment.severity());
        assertEquals(BigDecimal.ZERO, assessment.penaltyAmount());
    }

    @Test
    @DisplayName("Should assess overstay violation and calculate penalty")
    void shouldAssessOverstayViolation() {
        LocalDateTime validUntil = LocalDateTime.now().minusHours(2);
        LocalDateTime now = LocalDateTime.now();

        // Overstay 2 hours: Base 250 + 2 * 100 = 450.00
        var assessment = violationEngine.assessVisitorPass(validUntil, now, new BigDecimal("100.00"));
        assertTrue(assessment.isViolation());
        assertEquals(ParkingViolationEngine.ViolationSeverity.MINOR_OVERSTAY, assessment.severity());
        assertEquals(new BigDecimal("450.00"), assessment.penaltyAmount());
    }

    @Test
    @DisplayName("Should detect unauthorized slot occupation")
    void shouldDetectUnauthorizedOccupation() {
        var assessment = violationEngine.assessUnauthorizedSlotOccupation(10L, 99L, "A-101");
        assertTrue(assessment.isViolation());
        assertEquals(ParkingViolationEngine.ViolationSeverity.UNAUTHORIZED_SLOT_OCCUPATION, assessment.severity());
        assertEquals(new BigDecimal("500.00"), assessment.penaltyAmount());
    }
}
