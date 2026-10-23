package com.manacommunity.api.parking.ev.dto;

import com.manacommunity.api.parking.ev.entity.EvCharger;

import java.time.LocalDateTime;

public record EvChargerResponse(
        Long id,
        String deviceId,
        String slotNumber,
        String connectorType,
        Double maxKwRating,
        String status,
        LocalDateTime lastHeartbeatAt
) {
    public static EvChargerResponse from(EvCharger c) {
        return new EvChargerResponse(
                c.getId(),
                c.getDeviceId(),
                c.getParkingSlot() != null ? c.getParkingSlot().getSlotNumber() : null,
                c.getConnectorType(),
                c.getMaxKwRating(),
                c.getStatus().name(),
                c.getLastHeartbeatAt()
        );
    }
}