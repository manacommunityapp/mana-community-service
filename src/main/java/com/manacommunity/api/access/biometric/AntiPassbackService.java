package com.manacommunity.api.access.biometric;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Turnstile Anti-Passback & Tailgating Engine.
 *  - Hard blocks consecutive ENTRY swipes within 60s cooldown (window card-pass defense).
 *  - Performs Soft Anti-Passback override beyond cooldown or stale sessions (>4h) so sensor
 *    glitches / held doors never strand legitimate residents outside.
 *  - Pairs IR optical beam torso count (>1 person per flap cycle) to raise TAILGATE_DETECTED alerts.
 */
@Service
@RequiredArgsConstructor
public class AntiPassbackService {

    public static final long HARD_PASSBACK_COOLDOWN_SECONDS = 60L;
    public static final long STALE_ZONE_RESET_HOURS = 4L;

    private final TurnstileApbStateRepository apbRepo;

    @Builder
    public record ApbSwipeResult(
            boolean allowed,
            String decision,
            boolean softOverrideApplied,
            boolean tailgateDetected,
            boolean captureSecuritySnapshot,
            String currentState,
            String message
    ) {}

    @Transactional
    public ApbSwipeResult evaluateSwipe(
            Long communityId,
            String zoneId,
            String credentialId,
            String direction,
            String turnstileId,
            int irPersonsDetected,
            LocalDateTime eventTime
    ) {
        LocalDateTime now = eventTime != null ? eventTime : LocalDateTime.now();
        String dir = (direction != null && "EXIT".equalsIgnoreCase(direction)) ? "EXIT" : "ENTRY";
        boolean tailgate = irPersonsDetected > 1;

        TurnstileApbState state = apbRepo.findByCommunityIdAndZoneIdAndCredentialId(communityId, zoneId, credentialId)
                .orElseGet(() -> TurnstileApbState.builder()
                        .communityId(communityId)
                        .zoneId(zoneId)
                        .credentialId(credentialId)
                        .currentState("OUTSIDE")
                        .lastDirection("EXIT")
                        .build());

        if ("ENTRY".equals(dir) && "INSIDE".equals(state.getCurrentState())) {
            LocalDateTime lastEvent = state.getLastEventAt() != null ? state.getLastEventAt() : now;
            long secondsSinceLastEntry = Math.max(0L, Duration.between(lastEvent, now).getSeconds());

            // 1. Immediate card pass-through-window bounce (< 60s) -> Hard Block
            if (secondsSinceLastEntry < HARD_PASSBACK_COOLDOWN_SECONDS) {
                return ApbSwipeResult.builder()
                        .allowed(false)
                        .decision("DENIED_PASSBACK_COOLDOWN")
                        .softOverrideApplied(false)
                        .tailgateDetected(tailgate)
                        .captureSecuritySnapshot(true)
                        .currentState("INSIDE")
                        .message(String.format("Anti-passback cooldown active (%ds remaining): credential already badged IN at %s",
                                HARD_PASSBACK_COOLDOWN_SECONDS - secondsSinceLastEntry,
                                state.getLastTurnstileId()))
                        .build();
            }

            // 2. Missed exit / sensor glitch / stale session -> Soft APB Recovery (audit without lockout)
            state.setSoftOverrideCount(state.getSoftOverrideCount() + 1);
            state.setLastTurnstileId(turnstileId);
            state.setEnteredAt(now);
            state.setLastEventAt(now);
            state.setLastDirection("ENTRY");
            apbRepo.save(state);

            return ApbSwipeResult.builder()
                    .allowed(true)
                    .decision("GRANTED_SOFT_OVERRIDE")
                    .softOverrideApplied(true)
                    .tailgateDetected(tailgate)
                    .captureSecuritySnapshot(tailgate)
                    .currentState("INSIDE")
                    .message(tailgate
                            ? "Soft APB override applied + IR optical tailgate alert raised"
                            : "Soft anti-passback recovery entry granted (missed exit scan tolerated)")
                    .build();
        }

        // Standard clean ENTRY or EXIT transition
        state.setCurrentState("ENTRY".equals(dir) ? "INSIDE" : "OUTSIDE");
        state.setLastDirection(dir);
        state.setLastTurnstileId(turnstileId);
        state.setLastEventAt(now);
        if ("ENTRY".equals(dir)) {
            state.setEnteredAt(now);
        } else {
            state.setExitedAt(now);
        }
        apbRepo.save(state);

        return ApbSwipeResult.builder()
                .allowed(true)
                .decision(tailgate ? "GRANTED_WITH_TAILGATE_ALERT" : "GRANTED_OPEN")
                .softOverrideApplied(false)
                .tailgateDetected(tailgate)
                .captureSecuritySnapshot(tailgate)
                .currentState(state.getCurrentState())
                .message(tailgate
                        ? "Valid badge swipe but IR beam detected " + irPersonsDetected + " persons crossing — tailgate alert logged"
                        : "Turnstile access granted (" + dir + ")")
                .build();
    }
}
