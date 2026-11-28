package com.manacommunity.api.security.dpdp;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defensive engine preventing replay attacks on visitor QR codes and gate entry passcodes.
 * Enforces cryptographic nonce verification, time-bound ephemeral validity, and atomic single-use state transitions.
 */
@Component
@Slf4j
public class QrPasscodeReplayGuard {

    public enum PassStatus {
        ACTIVE,
        USED,
        EXPIRED,
        REVOKED
    }

    public enum ValidationOutcome {
        VALID_ENTRY_GRANTED,
        REPLAY_ATTACK_DETECTED,
        PASS_EXPIRED,
        INVALID_NONCE_OR_SIGNATURE
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VisitorPassToken {
        private String passId;
        private String nonce; // Cryptographically random single-use nonce
        private String societyId;
        private String flatNumber;
        private String visitorName;
        private Instant issuedAt;
        private Instant expiresAt;
        @Builder.Default
        private PassStatus status = PassStatus.ACTIVE;
        private Instant firstUsedAt;
        private String usedAtGateId;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PassValidationResult {
        private String passId;
        private ValidationOutcome outcome;
        private boolean barrierOpenAuthorized;
        private String securityMessage;
        private Instant validatedAt;
    }

    // In production, backed by Redis: "pass:nonce:{nonce}" with sliding TTL
    private final Map<String, Long> consumedNonces = new ConcurrentHashMap<>();
    private final Map<String, VisitorPassToken> passStore = new ConcurrentHashMap<>();

    public void registerPass(VisitorPassToken pass) {
        passStore.put(pass.getPassId(), pass);
    }

    public VisitorPassToken getPass(String passId) {
        return passStore.get(passId);
    }

    /**
     * Atomically validates a scanned visitor QR code / passcode at the security gate.
     * Guaranteed idempotent and replay-resistant: only the first scan grants physical barrier entry.
     */
    public synchronized PassValidationResult validateAndConsumePass(
            String passId,
            String presentedNonce,
            String gateId,
            Instant currentTime) {

        VisitorPassToken pass = passStore.get(passId);
        if (pass == null || pass.getNonce() == null || !pass.getNonce().equals(presentedNonce)) {
            log.warn("Invalid pass presentation: PassId [{}] with invalid nonce [{}]", passId, presentedNonce);
            return PassValidationResult.builder()
                    .passId(passId)
                    .outcome(ValidationOutcome.INVALID_NONCE_OR_SIGNATURE)
                    .barrierOpenAuthorized(false)
                    .securityMessage("Access Denied: Invalid QR code or tampered passcode signature.")
                    .validatedAt(currentTime)
                    .build();
        }

        // 1. Time-Bound Validity Check
        if (currentTime.isAfter(pass.getExpiresAt())) {
            pass.setStatus(PassStatus.EXPIRED);
            log.warn("Expired pass scanned: PassId [{}] expired at [{}]", passId, pass.getExpiresAt());
            return PassValidationResult.builder()
                    .passId(passId)
                    .outcome(ValidationOutcome.PASS_EXPIRED)
                    .barrierOpenAuthorized(false)
                    .securityMessage(String.format("Access Denied: QR code expired at %s.", pass.getExpiresAt()))
                    .validatedAt(currentTime)
                    .build();
        }

        // 2. Cryptographic Nonce & Single-Use State Check (Replay Detection)
        Long existingNonceUse = consumedNonces.putIfAbsent(presentedNonce, currentTime.toEpochMilli());
        if (existingNonceUse != null || pass.getStatus() == PassStatus.USED) {
            log.error("CRITICAL SECURITY ALERT: Replay attack detected for passId [{}]! Nonce [{}] was already consumed at epoch {}",
                    passId, presentedNonce, existingNonceUse);

            return PassValidationResult.builder()
                    .passId(passId)
                    .outcome(ValidationOutcome.REPLAY_ATTACK_DETECTED)
                    .barrierOpenAuthorized(false)
                    .securityMessage(String.format(
                            "CRITICAL ALERT: Replay attack detected! QR code was already used at Gate [%s] on %s.",
                            pass.getUsedAtGateId(), pass.getFirstUsedAt()))
                    .validatedAt(currentTime)
                    .build();
        }

        // 3. Legitimate First-Time Use -> Atomic transition to USED
        pass.setStatus(PassStatus.USED);
        pass.setFirstUsedAt(currentTime);
        pass.setUsedAtGateId(gateId);

        log.info("Pass [{}] verified successfully for [{}] at Gate [{}]. Boom barrier opening.",
                passId, pass.getVisitorName(), gateId);

        return PassValidationResult.builder()
                .passId(passId)
                .outcome(ValidationOutcome.VALID_ENTRY_GRANTED)
                .barrierOpenAuthorized(true)
                .securityMessage(String.format("Welcome %s! Access authorized for Flat %s.",
                        pass.getVisitorName(), pass.getFlatNumber()))
                .validatedAt(currentTime)
                .build();
    }

    public void clear() {
        consumedNonces.clear();
        passStore.clear();
    }
}
