package com.manacommunity.api.parking.ev.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record EvStartSessionRequest(
    @NotNull Long stationId,
    String vehicleNumber,
    BigDecimal initialMeterKwh
) {}
