package com.manacommunity.api.social.events.service;

import com.manacommunity.api.social.events.model.FestivalTicketDto;
import com.manacommunity.api.social.events.model.TicketRedemptionResult;

import java.time.Instant;

public interface FestivalTicketingAndRedemptionEngine {

    /**
     * Initializes total capacity cap for a community festival event.
     */
    void initializeEventCapacity(Long eventId, int totalCapacity);

    /**
     * Atomically reserves event headcount simulating Redis Lua token-bucket logic.
     * Returns true if capacity was successfully granted, false if sold out.
     */
    boolean reserveCapacity(Long eventId, int requestedHeadcount);

    /**
     * Issues a cryptographically signed ticket with an HMAC-SHA256 offline-verifiable QR token.
     */
    FestivalTicketDto issueTicket(Long eventId, Long userId, String residentName, String flatNumber,
                                  int headcount, int vegMeals, int nonVegMeals, Instant expiresAt);

    /**
     * Offline QR redemption at clubhouse gate:
     * 1. Verifies cryptographic signature using society public secret (zero network).
     * 2. Checks token expiration.
     * 3. Checks local anti-replay cache for duplicate scan attempts.
     * 4. Idempotently marks ticket as redeemed.
     */
    TicketRedemptionResult redeemTicketOffline(String qrToken, String gateStationId);

    /**
     * Returns remaining available capacity for an event.
     */
    int getRemainingCapacity(Long eventId);
}
