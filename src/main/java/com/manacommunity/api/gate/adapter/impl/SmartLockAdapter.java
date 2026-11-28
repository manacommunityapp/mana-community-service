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
 * Handles events from Wi-Fi / BLE smart door locks.
 *
 * Smart locks validate PINs offline (HKDF+HMAC-SHA256 via OfflineSmartLockPinService).
 * When an entry event arrives here, the lock already granted or denied access locally;
 * this adapter normalises that decision into the unified GateEventResult audit record.
 *
 * For outbound lock / unlock commands it calls the lock's HTTP relay API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmartLockAdapter implements GateHardwareAdapter {

    @Value("${gate.smart-lock.relay-base-url:}")
    private String relayBaseUrl;

    private final RestClient.Builder restClientBuilder;

    @Override
    public HardwareType hardwareType() {
        return HardwareType.SMART_LOCK;
    }

    @Override
    public GateEventResult process(RawHardwareEvent event) {
        // Smart lock reports event AFTER it has already acted (offline PIN validation).
        // metadata["status"] = GRANTED | DENIED; metadata["flat"] = flat number.
        Map<String, Object> meta = event.metadata() != null ? event.metadata() : Map.of();
        String status    = String.valueOf(meta.getOrDefault("status", "GRANTED"));
        String flatNumber= String.valueOf(meta.getOrDefault("flat", ""));
        String residentName = String.valueOf(meta.getOrDefault("residentName", ""));

        BarrierAction action = "DENIED".equalsIgnoreCase(status) ? BarrierAction.DENY : BarrierAction.OPEN;

        return GateEventResult.builder()
                .hardwareType(HardwareType.SMART_LOCK)
                .gateId(event.gateId())
                .direction(event.direction())
                .identifierType("PIN")
                .identifierValue("***")          // never log the actual PIN
                .matchedEntityType(action == BarrierAction.OPEN ? "RESIDENT" : "UNKNOWN")
                .matchedName(residentName.isBlank() ? null : residentName)
                .barrierAction(action)
                .notes(flatNumber.isBlank() ? null : "Flat " + flatNumber)
                .build();
    }

    @Override
    public BarrierCommandResult sendCommand(BarrierCommand command) {
        if (relayBaseUrl.isBlank()) {
            log.warn("gate.smart-lock.relay-base-url not configured; skipping command {} for {}",
                    command.action(), command.gateId());
            return BarrierCommandResult.builder()
                    .success(false)
                    .gateId(command.gateId())
                    .action(command.action())
                    .message("Smart lock relay URL not configured")
                    .build();
        }

        long start = System.currentTimeMillis();
        try {
            RestClient client = restClientBuilder.baseUrl(relayBaseUrl).build();
            ResponseEntity<Void> resp = client.post()
                    .uri("/locks/{gateId}/command", command.gateId())
                    .body(Map.of(
                            "action", command.action(),
                            "reason", command.reason() != null ? command.reason() : "",
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
            log.error("Smart lock relay command failed for gate {}: {}", command.gateId(), e.getMessage());
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
