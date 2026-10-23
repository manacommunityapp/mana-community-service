package com.manacommunity.api.parking.unit;

import com.manacommunity.api.parking.engine.ParkingMarketplaceEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Parking Marketplace Engine Unit Tests")
class ParkingMarketplaceEngineTest {

    private ParkingMarketplaceEngine marketplaceEngine;

    @BeforeEach
    void setUp() {
        marketplaceEngine = new ParkingMarketplaceEngine();
    }

    @Test
    @DisplayName("Should calculate daily rental with 10% society revenue split")
    void testPaidMarketplaceBooking() {
        LocalDate start = LocalDate.of(2026, 10, 10);
        LocalDate end = LocalDate.of(2026, 10, 14); // 5 days inclusive
        BigDecimal dailyRate = new BigDecimal("100.00");

        var result = marketplaceEngine.calculateBooking(start, end, dailyRate, false);

        assertEquals(5, result.totalDays());
        assertEquals(new BigDecimal("500.00"), result.grossAmount());
        assertEquals(new BigDecimal("50.00"), result.societyCommission());
        assertEquals(new BigDecimal("450.00"), result.ownerPayout());
        assertFalse(result.isGoodNeighbor());
    }

    @Test
    @DisplayName("Should calculate 0 fees for Good Neighbor free lending mode")
    void testGoodNeighborFreeLending() {
        LocalDate start = LocalDate.of(2026, 10, 10);
        LocalDate end = LocalDate.of(2026, 10, 15);

        var result = marketplaceEngine.calculateBooking(start, end, new BigDecimal("80.00"), true);

        assertEquals(6, result.totalDays());
        assertEquals(BigDecimal.ZERO, result.grossAmount());
        assertEquals(BigDecimal.ZERO, result.ownerPayout());
        assertTrue(result.isGoodNeighbor());
    }

    @Test
    @DisplayName("Should validate ANPR gate authorization within booking schedule")
    void testAnprGateScheduleValidation() {
        LocalDate start = LocalDate.of(2026, 10, 10);
        LocalDate end = LocalDate.of(2026, 10, 15);

        // Before start date
        assertFalse(marketplaceEngine.isAnprAuthorizedForSlot(start, end, LocalDate.of(2026, 10, 9)));

        // During booking
        assertTrue(marketplaceEngine.isAnprAuthorizedForSlot(start, end, LocalDate.of(2026, 10, 10)));
        assertTrue(marketplaceEngine.isAnprAuthorizedForSlot(start, end, LocalDate.of(2026, 10, 13)));
        assertTrue(marketplaceEngine.isAnprAuthorizedForSlot(start, end, LocalDate.of(2026, 10, 15)));

        // After booking end date
        assertFalse(marketplaceEngine.isAnprAuthorizedForSlot(start, end, LocalDate.of(2026, 10, 16)));
    }
}
