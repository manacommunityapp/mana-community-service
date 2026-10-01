package com.manacommunity.api.parking.ev.dto;

public record StopChargingRequest(
        Double finalMeterReading,
        String stopReason
) {}