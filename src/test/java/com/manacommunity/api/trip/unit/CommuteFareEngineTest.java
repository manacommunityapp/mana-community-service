package com.manacommunity.api.trip.unit;

import com.manacommunity.api.trip.engine.CommuteFareEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Commute Fare Engine Unit Tests")
class CommuteFareEngineTest {

    private CommuteFareEngine fareEngine;

    @BeforeEach
    void setUp() {
        fareEngine = new CommuteFareEngine();
    }

    @Test
    @DisplayName("Should calculate fair recommended fare per seat with toll")
    void shouldCalculateRecommendedFare() {
        // 20 km * 8 INR/km = 160 INR fuel + 40 INR toll = 200 INR total trip cost
        // 3 seats + 1 driver = 4 occupants -> 200 / 4 = 50.00 INR per seat
        BigDecimal fare = fareEngine.calculateRecommendedFare(20.0, 3, new BigDecimal("40.00"), false);
        assertEquals(new BigDecimal("50.00"), fare);
    }

    @Test
    @DisplayName("Should return zero for zero or negative distance")
    void shouldReturnZeroForInvalidDistance() {
        BigDecimal fare = fareEngine.calculateRecommendedFare(0.0, 3, BigDecimal.ZERO, false);
        assertEquals(BigDecimal.ZERO, fare);
    }

    @Test
    @DisplayName("Should validate fair pricing limits correctly")
    void shouldValidateFairLimits() {
        // Distance 20km, 3 seats -> recommended ~44 INR with AC -> max cap ~66 INR
        assertTrue(fareEngine.isFareWithinFairLimits(new BigDecimal("50.00"), 20.0, 3));
        assertTrue(fareEngine.isFareWithinFairLimits(BigDecimal.ZERO, 20.0, 3)); // free ride is always valid
        assertFalse(fareEngine.isFareWithinFairLimits(new BigDecimal("300.00"), 20.0, 3)); // commercial surge disallowed
    }

    @Test
    @DisplayName("Should calculate total booking amount with discount")
    void shouldCalculateBookingTotalWithDiscount() {
        // 2 seats * 50 INR = 100 INR with 10% discount -> 90.00 INR
        BigDecimal total = fareEngine.calculateBookingTotal(new BigDecimal("50.00"), 2, new BigDecimal("10.00"));
        assertEquals(new BigDecimal("90.00"), total);
    }
}
