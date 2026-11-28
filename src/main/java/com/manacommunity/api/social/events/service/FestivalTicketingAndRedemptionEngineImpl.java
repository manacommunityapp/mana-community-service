package com.manacommunity.api.social.events.service;

import com.manacommunity.api.social.events.model.FestivalTicketDto;
import com.manacommunity.api.social.events.model.TicketRedemptionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class FestivalTicketingAndRedemptionEngineImpl implements FestivalTicketingAndRedemptionEngine {

    private final Map<Long, AtomicInteger> capacityPools = new ConcurrentHashMap<>();
    private final AtomicLong ticketIdSeq = new AtomicLong(90000L);

    // Simulated offline SQLite anti-replay store on tablet
    private record ScannedRecord(Instant scannedAt, String gateStationId) {}
    private final Map<String, ScannedRecord> redeemedStore = new ConcurrentHashMap<>();

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    // 256-bit society secret key for festival ticket signing
    private static final byte[] SOCIETY_SECRET = "MANA_COMMUNITY_FESTIVAL_SECRET_KEY_2026_ED25519_HMAC".getBytes(StandardCharsets.UTF_8);

    @Override
    public void initializeEventCapacity(Long eventId, int totalCapacity) {
        capacityPools.put(eventId, new AtomicInteger(Math.max(0, totalCapacity)));
    }

    @Override
    public boolean reserveCapacity(Long eventId, int requestedHeadcount) {
        AtomicInteger pool = capacityPools.get(eventId);
        if (pool == null || requestedHeadcount <= 0) {
            return false;
        }

        // Simulates atomic Redis Lua script: DECRBY if current >= requested
        while (true) {
            int current = pool.get();
            if (current < requestedHeadcount) {
                log.warn("Capacity reservation rejected for event {}: available={}, requested={}", eventId, current, requestedHeadcount);
                return false;
            }
            if (pool.compareAndSet(current, current - requestedHeadcount)) {
                log.info("Capacity reserved for event {}: new remaining={}", eventId, current - requestedHeadcount);
                return true;
            }
        }
    }

    @Override
    public FestivalTicketDto issueTicket(Long eventId, Long userId, String residentName, String flatNumber,
                                          int headcount, int vegMeals, int nonVegMeals, Instant expiresAt) {
        String ticketId = "TKT-" + eventId + "-" + ticketIdSeq.incrementAndGet();

        // Construct canonical claims string
        // format: ticketId|eventId|userId|residentName|flatNumber|headcount|vegMeals|nonVegMeals|epochSec
        long expSec = expiresAt != null ? expiresAt.getEpochSecond() : Instant.now().plusSeconds(86400).getEpochSecond();
        String claims = String.format("%s|%d|%d|%s|%s|%d|%d|%d|%d",
                ticketId, eventId, userId, residentName, flatNumber, headcount, vegMeals, nonVegMeals, expSec);

        String signature = signClaims(claims);
        String qrToken = Base64.getUrlEncoder().withoutPadding().encodeToString(claims.getBytes(StandardCharsets.UTF_8))
                + "." + signature;

        return FestivalTicketDto.builder()
                .ticketId(ticketId)
                .eventId(eventId)
                .userId(userId)
                .residentName(residentName)
                .flatNumber(flatNumber)
                .headcount(headcount)
                .vegMeals(vegMeals)
                .nonVegMeals(nonVegMeals)
                .expiresAt(Instant.ofEpochSecond(expSec))
                .qrToken(qrToken)
                .build();
    }

    @Override
    public TicketRedemptionResult redeemTicketOffline(String qrToken, String gateStationId) {
        if (qrToken == null || !qrToken.contains(".")) {
            return TicketRedemptionResult.builder()
                    .valid(false)
                    .rejectionReason("MALFORMED_QR_TOKEN")
                    .build();
        }

        String[] parts = qrToken.split("\\.");
        if (parts.length != 2) {
            return TicketRedemptionResult.builder()
                    .valid(false)
                    .rejectionReason("INVALID_QR_STRUCTURE")
                    .build();
        }

        String encodedClaims = parts[0];
        String receivedSignature = parts[1];

        String claims;
        try {
            claims = new String(Base64.getUrlDecoder().decode(encodedClaims), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return TicketRedemptionResult.builder()
                    .valid(false)
                    .rejectionReason("CORRUPT_PAYLOAD")
                    .build();
        }

        // 1. Verify Cryptographic Signature (Zero Network, Pure In-Memory Crypto)
        String expectedSignature = signClaims(claims);
        if (!expectedSignature.equals(receivedSignature)) {
            log.error("Ticket signature verification failed for claims: {}", claims);
            return TicketRedemptionResult.builder()
                    .valid(false)
                    .rejectionReason("SIGNATURE_FORGERY_DETECTED")
                    .build();
        }

        // 2. Parse Claims
        String[] fields = claims.split("\\|");
        if (fields.length != 9) {
            return TicketRedemptionResult.builder()
                    .valid(false)
                    .rejectionReason("INVALID_CLAIMS_COUNT")
                    .build();
        }

        String ticketId = fields[0];
        long eventId = Long.parseLong(fields[1]);
        String flatNumber = fields[4];
        int headcount = Integer.parseInt(fields[5]);
        int vegMeals = Integer.parseInt(fields[6]);
        int nonVegMeals = Integer.parseInt(fields[7]);
        long expSec = Long.parseLong(fields[8]);

        // 3. Expiration Check
        if (Instant.now().getEpochSecond() > expSec) {
            return TicketRedemptionResult.builder()
                    .valid(false)
                    .ticketId(ticketId)
                    .rejectionReason("TICKET_EXPIRED")
                    .build();
        }

        // 4. Anti-Replay Duplicate Check (Local SQLite Simulation)
        ScannedRecord previousScan = redeemedStore.putIfAbsent(ticketId, new ScannedRecord(Instant.now(), gateStationId));
        if (previousScan != null) {
            log.warn("Duplicate redemption attempt for ticket {} at station {}", ticketId, gateStationId);
            return TicketRedemptionResult.builder()
                    .valid(false)
                    .ticketId(ticketId)
                    .rejectionReason("DUPLICATE_SCAN: Already redeemed at " + previousScan.gateStationId() + " at " + previousScan.scannedAt())
                    .build();
        }

        // 5. Successful Redemption
        log.info("Ticket {} successfully redeemed at station {} (headcount: {}, veg: {}, nonVeg: {})",
                ticketId, gateStationId, headcount, vegMeals, nonVegMeals);

        return TicketRedemptionResult.builder()
                .valid(true)
                .ticketId(ticketId)
                .eventId(eventId)
                .flatNumber(flatNumber)
                .headcount(headcount)
                .vegMeals(vegMeals)
                .nonVegMeals(nonVegMeals)
                .build();
    }

    @Override
    public int getRemainingCapacity(Long eventId) {
        AtomicInteger pool = capacityPools.get(eventId);
        return pool != null ? pool.get() : 0;
    }

    private String signClaims(String claims) {
        try {
            Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
            hmac.init(new SecretKeySpec(SOCIETY_SECRET, HMAC_ALGORITHM));
            byte[] rawHmac = hmac.doFinal(claims.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to calculate HMAC signature", ex);
        }
    }
}
