package com.manacommunity.api.gate.adapter;

import lombok.Builder;

/**
 * Adapter processing result — hardware-agnostic decision record.
 * The GateHardwareDispatcher persists this as a GateEvent entity.
 */
@Builder
public record GateEventResult(
        HardwareType hardwareType,
        String gateId,
        String direction,
        /** PLATE | RFID_TAG | QR_TOKEN | PIN | MANUAL */
        String identifierType,
        String identifierValue,
        /** RESIDENT | VISITOR | UNKNOWN */
        String matchedEntityType,
        Long matchedEntityId,
        String matchedName,
        BarrierAction barrierAction,
        String notes
) {}
