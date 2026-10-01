package com.manacommunity.api.parking.ev.dto;

import com.manacommunity.api.parking.ev.enums.EvPaymentStatus;
import com.manacommunity.api.parking.ev.enums.EvSessionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EvSessionResponse(
    Long id,
    Long stationId,
    Long residentId,
    Long communityId,
    String vehicleNumber,
    LocalDateTime startTime,
    LocalDateTime endTime,
    BigDecimal initialMeterKwh,
    BigDecimal finalMeterKwh,
    BigDecimal totalEnergyKwh,
    BigDecimal peakPowerKw,
    Integer chargingDurationMinutes,
    Integer idleDurationMinutes,
    BigDecimal energyCost,
    BigDecimal idlePenalty,
    BigDecimal totalCost,
    EvSessionStatus status,
    EvPaymentStatus paymentStatus,
    Long walletTransactionId,
    String stopReason
) {}
