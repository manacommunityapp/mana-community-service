package com.manacommunity.api.parking.ev.dto;

public record StartChargingRequest(
        Long chargerId,
        Long vehicleId,
        Double currentMeterReading
) {}