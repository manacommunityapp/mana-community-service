package com.manacommunity.api.parking.ev.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EvTelemetryRequest(
    @NotBlank String deviceId,
    @NotNull Double meterReadingKwh,
    BigDecimal powerKw,
    BigDecimal voltage,
    BigDecimal currentAmps,
    BigDecimal temperatureCelsius,
    BigDecimal stateOfChargePercent,
    String errorCode,
    LocalDateTime timestamp
) {}
