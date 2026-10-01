package com.manacommunity.api.parking.ev.dto;

import com.manacommunity.api.parking.ev.entity.EvCharger.ChargerStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EvLiveTelemetryResponse(
    Long chargerId,
    String deviceId,
    ChargerStatus status,
    BigDecimal currentPowerKw,
    BigDecimal voltage,
    BigDecimal currentAmps,
    BigDecimal temperatureCelsius,
    Double meterReadingKwh,
    BigDecimal stateOfChargePercent,
    LocalDateTime timestamp
) {}
