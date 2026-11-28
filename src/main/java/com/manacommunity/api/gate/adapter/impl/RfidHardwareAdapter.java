package com.manacommunity.api.gate.adapter.impl;

import com.manacommunity.api.gate.adapter.*;
import com.manacommunity.api.gate.model.RfidTagMapping;
import com.manacommunity.api.gate.repository.RfidTagMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RfidHardwareAdapter implements GateHardwareAdapter {

    private final RfidTagMappingRepository rfidTagMappingRepository;

    @Override
    public HardwareType hardwareType() {
        return HardwareType.RFID;
    }

    @Override
    public GateEventResult process(RawHardwareEvent event) {
        Optional<RfidTagMapping> mapping = rfidTagMappingRepository
                .findByCommunityIdAndRfidTagIdAndActiveTrue(
                        event.communityId(), event.identifierValue());

        if (mapping.isPresent()) {
            RfidTagMapping tag = mapping.get();
            return GateEventResult.builder()
                    .hardwareType(HardwareType.RFID)
                    .gateId(event.gateId())
                    .direction(event.direction())
                    .identifierType("RFID_TAG")
                    .identifierValue(event.identifierValue())
                    .matchedEntityType("RESIDENT")
                    .matchedEntityId(tag.getResidentId())
                    .matchedName(tag.getResidentName())
                    .barrierAction(BarrierAction.OPEN)
                    .notes("Flat " + tag.getFlatNumber())
                    .build();
        }

        log.warn("Unknown RFID tag {} at gate {} community {}",
                event.identifierValue(), event.gateId(), event.communityId());

        return GateEventResult.builder()
                .hardwareType(HardwareType.RFID)
                .gateId(event.gateId())
                .direction(event.direction())
                .identifierType("RFID_TAG")
                .identifierValue(event.identifierValue())
                .matchedEntityType("UNKNOWN")
                .barrierAction(BarrierAction.HOLD)
                .notes("Unregistered RFID tag")
                .build();
    }

    @Override
    public BarrierCommandResult sendCommand(BarrierCommand command) {
        log.debug("RFID reader at gate {} does not accept remote commands", command.gateId());
        return BarrierCommandResult.builder()
                .success(false)
                .gateId(command.gateId())
                .action(command.action())
                .message("RFID reader does not support barrier commands")
                .build();
    }
}
