package com.manacommunity.api.social.events;

import com.manacommunity.api.social.events.model.FestivalTicketDto;
import com.manacommunity.api.social.events.model.TicketRedemptionResult;
import com.manacommunity.api.social.events.service.FestivalTicketingAndRedemptionEngine;
import com.manacommunity.api.social.events.service.FestivalTicketingAndRedemptionEngineImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class FestivalTicketingAndRedemptionEngineTest {

    private FestivalTicketingAndRedemptionEngine ticketingEngine;
    private static final Long EVENT_ID = 501L;

    @BeforeEach
    void setUp() {
        ticketingEngine = new FestivalTicketingAndRedemptionEngineImpl();
        // Initialize Diwali Dinner with total capacity cap of 10 headcounts
        ticketingEngine.initializeEventCapacity(EVENT_ID, 10);
    }

    @Test
    @DisplayName("Atomic capacity reservation manages flash-crowd rush and rejects overselling")
    void testAtomicCapacityReservation() {
        assertEquals(10, ticketingEngine.getRemainingCapacity(EVENT_ID));

        // Family 1 books 4 spots -> 6 left
        boolean res1 = ticketingEngine.reserveCapacity(EVENT_ID, 4);
        assertTrue(res1);
        assertEquals(6, ticketingEngine.getRemainingCapacity(EVENT_ID));

        // Family 2 books 4 spots -> 2 left
        boolean res2 = ticketingEngine.reserveCapacity(EVENT_ID, 4);
        assertTrue(res2);
        assertEquals(2, ticketingEngine.getRemainingCapacity(EVENT_ID));

        // Family 3 attempts 4 spots -> REJECTED (only 2 left)
        boolean res3 = ticketingEngine.reserveCapacity(EVENT_ID, 4);
        assertFalse(res3, "Should reject reservation exceeding remaining capacity");
        assertEquals(2, ticketingEngine.getRemainingCapacity(EVENT_ID), "Remaining capacity must not be corrupted");

        // Family 4 books remaining 2 spots -> 0 left
        boolean res4 = ticketingEngine.reserveCapacity(EVENT_ID, 2);
        assertTrue(res4);
        assertEquals(0, ticketingEngine.getRemainingCapacity(EVENT_ID));
    }

    @Test
    @DisplayName("Cryptographic offline QR code is verified and redeemed in local SQLite anti-replay store")
    void testValidOfflineQrRedemption() {
        FestivalTicketDto ticket = ticketingEngine.issueTicket(
                EVENT_ID, 801L, "Ramesh Varma", "Tower A - 302",
                4, 3, 1, Instant.now().plus(2, ChronoUnit.HOURS)
        );

        assertNotNull(ticket.getQrToken());
        assertTrue(ticket.getQrToken().contains("."));

        // Scan at Gate 1
        TicketRedemptionResult result = ticketingEngine.redeemTicketOffline(ticket.getQrToken(), "GATE_STATION_1");
        assertTrue(result.isValid());
        assertEquals(ticket.getTicketId(), result.getTicketId());
        assertEquals("Tower A - 302", result.getFlatNumber());
        assertEquals(4, result.getHeadcount());
        assertEquals(3, result.getVegMeals());
        assertEquals(1, result.getNonVegMeals());
    }

    @Test
    @DisplayName("Anti-replay defense rejects duplicate scan attempts at different gates")
    void testDuplicateScanRejection() {
        FestivalTicketDto ticket = ticketingEngine.issueTicket(
                EVENT_ID, 802L, "Kavita Rao", "Tower C - 1001",
                2, 2, 0, Instant.now().plus(2, ChronoUnit.HOURS)
        );

        // First scan at Gate 1 (Clubhouse Entrance) -> APPROVED
        TicketRedemptionResult firstScan = ticketingEngine.redeemTicketOffline(ticket.getQrToken(), "GATE_1");
        assertTrue(firstScan.isValid());

        // Duplicate scan at Gate 2 (Lawn Entrance) using same QR -> REJECTED
        TicketRedemptionResult secondScan = ticketingEngine.redeemTicketOffline(ticket.getQrToken(), "GATE_2");
        assertFalse(secondScan.isValid());
        assertNotNull(secondScan.getRejectionReason());
        assertTrue(secondScan.getRejectionReason().contains("DUPLICATE_SCAN"));
        assertTrue(secondScan.getRejectionReason().contains("GATE_1"));
    }

    @Test
    @DisplayName("Cryptographic tampering of QR token is immediately detected and rejected")
    void testTamperedQrRejection() {
        FestivalTicketDto ticket = ticketingEngine.issueTicket(
                EVENT_ID, 803L, "Anand Kumar", "Tower B - 204",
                2, 2, 0, Instant.now().plus(2, ChronoUnit.HOURS)
        );

        String[] parts = ticket.getQrToken().split("\\.");
        String encodedClaims = parts[0];
        String validSignature = parts[1];

        // Attacker decodes claims and tampers headcount from 2 to 10
        String decodedClaims = new String(Base64.getUrlDecoder().decode(encodedClaims));
        String tamperedClaims = decodedClaims.replace("|2|2|0|", "|10|10|0|");
        String tamperedEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(tamperedClaims.getBytes());
        String forgedToken = tamperedEncoded + "." + validSignature;

        TicketRedemptionResult scanResult = ticketingEngine.redeemTicketOffline(forgedToken, "GATE_1");
        assertFalse(scanResult.isValid());
        assertEquals("SIGNATURE_FORGERY_DETECTED", scanResult.getRejectionReason());
    }
}
