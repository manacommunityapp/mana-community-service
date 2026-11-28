package com.manacommunity.api.edgehardware.service;

import com.manacommunity.api.edgehardware.entity.EdgeBarrierCommandNonce;
import com.manacommunity.api.edgehardware.repository.EdgeBarrierCommandNonceRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

/**
 * RS-485 Boom Barrier Pulse Safety & Anti-Tailgating State Machine:
 * - Blocks MQTT/WebSocket duplicate command redeliveries (e.g. 8s jitter retry) via persistent command nonces.
 * - Rejects stale offline-queued open commands (>15s TTL) so expired approvals never open for Car #2.
 * - Requires dedicated OPEN_ONLY / CLOSE terminals paired with Loop A (approach) and Loop B (safety-under-boom).
 */
@Service
@RequiredArgsConstructor
public class BarrierRelaySafetyService {

    public static final int DEFAULT_CMD_TTL_SECONDS = 15;
    public static final int RELAY_PULSE_MS = 500;

    private final EdgeBarrierCommandNonceRepository nonceRepo;

    @Builder
    public record BarrierCommandRequest(
            String controllerId,
            String cmdNonce,
            String commandType, // OPEN_ONLY, CLOSE
            boolean loopAOccupied,
            boolean loopBOccupied,
            Instant issuedAt,
            int ttlSeconds
    ) {}

    @Builder
    public record BarrierCommandOutcome(
            String controllerId,
            String cmdNonce,
            String outcome, // RELAY_PULSED, IDEMPOTENT_DUPLICATE, STALE_COMMAND_EXPIRED, NO_VEHICLE_ON_LOOP_A, LOOP_B_SAFETY_INHIBIT
            int relayPulseMs,
            String relayCoilTarget,
            boolean hardwareInterlockEngaged
    ) {}

    @Builder
    public record LoopCycleOutcome(
            String controllerId,
            boolean tailgateViolation,
            boolean cycleCompletedReset,
            String alertCode
    ) {}

    @Transactional
    public BarrierCommandOutcome executeBarrierCommand(BarrierCommandRequest req, Instant nowOverride) {
        Instant now = nowOverride != null ? nowOverride : Instant.now();
        String controller = req.controllerId().trim().toUpperCase(Locale.ROOT);
        String nonce = req.cmdNonce().trim();
        String cmdType = req.commandType() != null ? req.commandType().trim().toUpperCase(Locale.ROOT) : "OPEN_ONLY";

        if ("TOGGLE".equals(cmdType)) {
            throw new IllegalArgumentException("Unsafe TOGGLE command forbidden; wire dedicated OPEN_ONLY / CLOSE coils");
        }

        // 1. Idempotency protection: 8s MQTT QoS1 redelivery cannot double-pulse the relay
        var seen = nonceRepo.findByControllerIdAndCmdNonce(controller, nonce);
        if (seen.isPresent()) {
            return BarrierCommandOutcome.builder()
                    .controllerId(controller)
                    .cmdNonce(nonce)
                    .outcome("IDEMPOTENT_DUPLICATE")
                    .relayPulseMs(0)
                    .relayCoilTarget("NONE")
                    .hardwareInterlockEngaged(req.loopBOccupied())
                    .build();
        }

        // 2. Reject stale late-arriving commands past TTL
        int effectiveTtl = req.ttlSeconds() > 0 ? req.ttlSeconds() : DEFAULT_CMD_TTL_SECONDS;
        if (req.issuedAt() == null || now.getEpochSecond() - req.issuedAt().getEpochSecond() > effectiveTtl) {
            return BarrierCommandOutcome.builder()
                    .controllerId(controller)
                    .cmdNonce(nonce)
                    .outcome("STALE_COMMAND_EXPIRED")
                    .relayPulseMs(0)
                    .relayCoilTarget("NONE")
                    .hardwareInterlockEngaged(req.loopBOccupied())
                    .build();
        }

        // 3. Hardware safety-loop & approach-loop interlock checks
        if ("CLOSE".equals(cmdType) && req.loopBOccupied()) {
            return BarrierCommandOutcome.builder()
                    .controllerId(controller)
                    .cmdNonce(nonce)
                    .outcome("LOOP_B_SAFETY_INHIBIT")
                    .relayPulseMs(0)
                    .relayCoilTarget("NONE")
                    .hardwareInterlockEngaged(true)
                    .build();
        }

        if ("OPEN_ONLY".equals(cmdType) && !req.loopAOccupied()) {
            return BarrierCommandOutcome.builder()
                    .controllerId(controller)
                    .cmdNonce(nonce)
                    .outcome("NO_VEHICLE_ON_LOOP_A")
                    .relayPulseMs(0)
                    .relayCoilTarget("NONE")
                    .hardwareInterlockEngaged(req.loopBOccupied())
                    .build();
        }

        // 4. Consume nonce durably and pulse dedicated dry-contact coil for 500ms
        nonceRepo.save(EdgeBarrierCommandNonce.builder()
                .controllerId(controller)
                .cmdNonce(nonce)
                .commandType(cmdType)
                .loopAOccupied(req.loopAOccupied())
                .loopBOccupied(req.loopBOccupied())
                .outcome("RELAY_PULSED")
                .tailgateDetected(false)
                .issuedAt(req.issuedAt())
                .build());

        return BarrierCommandOutcome.builder()
                .controllerId(controller)
                .cmdNonce(nonce)
                .outcome("RELAY_PULSED")
                .relayPulseMs(RELAY_PULSE_MS)
                .relayCoilTarget("OPEN_ONLY".equals(cmdType) ? "COIL_OPEN_DRY_CONTACT" : "COIL_CLOSE_DRY_CONTACT")
                .hardwareInterlockEngaged(req.loopBOccupied())
                .build();
    }

    public LoopCycleOutcome evaluateLoopTransition(
            String controllerId,
            boolean previousLoopB,
            boolean currentLoopA,
            boolean currentLoopB,
            boolean authorizedCycleActive
    ) {
        if (!authorizedCycleActive && (currentLoopA && currentLoopB)) {
            return LoopCycleOutcome.builder()
                    .controllerId(controllerId)
                    .tailgateViolation(true)
                    .cycleCompletedReset(false)
                    .alertCode("TAILGATE_VIOLATION")
                    .build();
        }
        boolean cleared = previousLoopB && !currentLoopB;
        return LoopCycleOutcome.builder()
                .controllerId(controllerId)
                .tailgateViolation(false)
                .cycleCompletedReset(cleared)
                .alertCode(cleared ? "CYCLE_CLEARED_CLOSING" : "NOMINAL")
                .build();
    }
}
