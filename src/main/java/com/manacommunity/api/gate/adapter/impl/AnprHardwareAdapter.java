package com.manacommunity.api.gate.adapter.impl;

import com.manacommunity.api.gate.adapter.*;
import com.manacommunity.api.parking.anpr.dto.AnprWebhookPayload;
import com.manacommunity.api.parking.anpr.dto.AnprWebhookResponse;
import com.manacommunity.api.parking.anpr.service.AnprGateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnprHardwareAdapter implements GateHardwareAdapter {

    private final AnprGateService anprGateService;

    @Override
    public HardwareType hardwareType() {
        return HardwareType.ANPR;
    }

    @Override
    public GateEventResult process(RawHardwareEvent event) {
        AnprWebhookPayload payload = new AnprWebhookPayload(
                null,
                event.gateId(),
                event.direction(),
                event.identifierValue(),
                event.confidence(),
                "DETECTED",
                null,
                "gate-hardware-dispatcher"
        );

        AnprWebhookResponse response = anprGateService.processWebhook(event.communityId(), payload);

        BarrierAction action = mapBarrierAction(response.barrierAction());

        return GateEventResult.builder()
                .hardwareType(HardwareType.ANPR)
                .gateId(event.gateId())
                .direction(event.direction())
                .identifierType("PLATE")
                .identifierValue(event.identifierValue())
                .matchedEntityType(action == BarrierAction.OPEN ? "RESIDENT" : "UNKNOWN")
                .matchedName(response.matchedResidentName())
                .barrierAction(action)
                .notes(response.message())
                .build();
    }

    @Override
    public BarrierCommandResult sendCommand(BarrierCommand command) {
        // ANPR cameras are read-only sensors; barrier relay is a separate device
        log.debug("ANPR adapter ignoring barrier command {} for gate {}", command.action(), command.gateId());
        return BarrierCommandResult.builder()
                .success(false)
                .gateId(command.gateId())
                .action(command.action())
                .message("ANPR adapter does not support barrier commands directly")
                .build();
    }

    private BarrierAction mapBarrierAction(
            com.manacommunity.api.parking.anpr.entity.AnprGateEvent.BarrierAction anprAction) {
        if (anprAction == null) return BarrierAction.HOLD;
        return switch (anprAction) {
            case OPEN -> BarrierAction.OPEN;
            case DENY -> BarrierAction.DENY;
            default   -> BarrierAction.HOLD;
        };
    }
}
