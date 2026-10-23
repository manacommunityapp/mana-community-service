package com.manacommunity.api.parking.ev.dto;

import com.manacommunity.api.parking.ev.entity.EvChargingSession;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EvChargingSessionResponse(
        Long id,
        Long chargerId,
        String chargerDeviceId,
        String slotNumber,
        Long residentId,
        String residentName,
        String vehicleNumber,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Double startMeterKwh,
        Double endMeterKwh,
        Double totalKwh,
        BigDecimal totalCost,
        String status,
        Long cfbosWalletTransactionId,
        String stopReason
) {
    public static EvChargingSessionResponse from(EvChargingSession s) {
        return new EvChargingSessionResponse(
                s.getId(),
                s.getCharger().getId(),
                s.getCharger().getDeviceId(),
                s.getCharger().getParkingSlot() != null ? s.getCharger().getParkingSlot().getSlotNumber() : null,
                s.getResident().getId(),
                s.getResident().getFullName(),
                s.getVehicle() != null ? s.getVehicle().getNumberPlate() : null,
                s.getStartedAt(),
                s.getEndedAt(),
                s.getStartMeterKwh(),
                s.getEndMeterKwh(),
                s.getTotalKwh(),
                s.getTotalCost(),
                s.getStatus().name(),
                s.getCfbosWalletTransactionId(),
                s.getStopReason()
        );
    }
}