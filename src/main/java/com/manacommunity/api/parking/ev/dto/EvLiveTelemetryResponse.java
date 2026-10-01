package com.manacommunity.api.parking.ev.dto;

import com.manacommunity.api.parking.ev.enums.EvStationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EvLiveTelemetryResponse(
    Long stationId,
    String hardwareId,
    EvStationStatus status,
    BigDecimal currentPowerKw,
    BigDecimal voltage,
    BigDecimal currentAmps,
    BigDecimal temperatureCelsius,
    BigDecimal meterReadingKwh,
    BigDecimal stateOfChargePercent,
    LocalDateTime timestamp
) {}
