package com.manacommunity.api.gate.adapter;

import lombok.Builder;

import java.util.Map;

/**
 * Vendor-agnostic inbound event from any gate hardware device.
 * The dispatcher routes this to the matching GateHardwareAdapter based on hardwareType.
 */
@Builder
public record RawHardwareEvent(
        HardwareType hardwareType,
        Long communityId,
        String gateId,
        /** ENTRY or EXIT */
        String direction,
        /** The hardware-specific identifier: plate number, RFID tag ID, QR token, etc. */
        String identifierValue,
        /** Confidence score 0.0–1.0 (meaningful for ANPR). */
        Double confidence,
        /** Vendor-specific extras (camera ID, firmware version, etc.). */
        Map<String, Object> metadata
) {}
