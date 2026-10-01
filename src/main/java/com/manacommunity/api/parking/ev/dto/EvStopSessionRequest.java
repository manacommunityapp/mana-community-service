package com.manacommunity.api.parking.ev.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record EvStopSessionRequest(
    @NotNull BigDecimal finalMeterKwh,
    String stopReason,
    Integer idleDurationMinutes
) {}
