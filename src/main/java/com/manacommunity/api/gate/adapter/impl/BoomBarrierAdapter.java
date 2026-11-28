package com.manacommunity.api.gate.adapter.impl;

import com.manacommunity.api.gate.adapter.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Relay-controlled boom barrier — command-only device.
 *
 * Boom barriers don't generate access events; the ANPR or RFID adapter triggers the access
 * decision, and the GateHardwareDispatcher calls {@code sendCommand(OPEN)} here when the
 * decision is {@link BarrierAction#OPEN}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BoomBarrierAdapter implements GateHardwareAdapter {

    @Value("${gate.boom-barrier.relay-base-url:}")
    private String relayBaseUrl;

    private final RestClient.Builder restClientBuilder;

    @Override
    public HardwareType hardwareType() {
        return HardwareType.BOOM_BARRIER;
    }

    /** Boom barriers do not originate access events; this path should not be called. */
    @Override
    public GateEventResult process(RawHardwareEvent event) {
        return GateEventResult.builder()
                .hardwareType(HardwareType.BOOM_BARRIER)
                .gateId(event.gateId())
                .direction(event.direction())
                .identifierType("MANUAL")
                .identifierValue(event.identifierValue())
                .matchedEntityType("UNKNOWN")
                .barrierAction(BarrierAction.HOLD)
                .notes("Boom barrier does not originate events")
                .build();
    }

    @Override
    public BarrierCommandResult sendCommand(BarrierCommand command) {
        if (relayBaseUrl.isBlank()) {
            log.warn("gate.boom-barrier.relay-base-url not configured; skipping {} for gate {}",
                    command.action(), command.gateId());
            return BarrierCommandResult.builder()
                    .success(false)
                    .gateId(command.gateId())
                    .action(command.action())
                    .message("Boom barrier relay URL not configured")
                    .build();
        }

        long start = System.currentTimeMillis();
        try {
            RestClient client = restClientBuilder.baseUrl(relayBaseUrl).build();
            ResponseEntity<Void> resp = client.post()
                    .uri("/barriers/{gateId}/trigger", command.gateId())
                    .body(Map.of(
                            "action",      command.action(),
                            "communityId", command.communityId(),
                            "reason",      command.reason() != null ? command.reason() : "",
                            "triggeredBy", command.triggeredBy() != null ? command.triggeredBy() : "system"
                    ))
                    .retrieve()
                    .toBodilessEntity();

            return BarrierCommandResult.builder()
                    .success(resp.getStatusCode().is2xxSuccessful())
                    .gateId(command.gateId())
                    .action(command.action())
                    .message("HTTP " + resp.getStatusCode())
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        } catch (RestClientException e) {
            log.error("Boom barrier relay failed for gate {}: {}", command.gateId(), e.getMessage());
            return BarrierCommandResult.builder()
                    .success(false)
                    .gateId(command.gateId())
                    .action(command.action())
                    .message(e.getMessage())
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        }
    }
}
