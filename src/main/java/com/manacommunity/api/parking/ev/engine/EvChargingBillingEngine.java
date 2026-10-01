package com.manacommunity.api.parking.ev.engine;

import com.manacommunity.api.parking.ev.entity.EvTariffConfig;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
public class EvChargingBillingEngine {

    public record BillBreakdown(
        BigDecimal energyKwh,
        BigDecimal energyCost,
        BigDecimal idlePenalty,
        BigDecimal totalCost
    ) {}

    public BillBreakdown calculateBill(BigDecimal initialMeter, BigDecimal finalMeter,
                                      LocalDateTime startTime, LocalDateTime endTime,
                                      Integer idleMinutes, EvTariffConfig tariff) {
        BigDecimal baseRate = (tariff != null && tariff.getBaseRatePerKwh() != null)
                ? tariff.getBaseRatePerKwh()
                : new BigDecimal("8.50");

        BigDecimal peakRate = (tariff != null && tariff.getPeakHourRatePerKwh() != null)
                ? tariff.getPeakHourRatePerKwh()
                : new BigDecimal("12.00");

        LocalTime peakStart = (tariff != null && tariff.getPeakHourStart() != null)
                ? tariff.getPeakHourStart()
                : LocalTime.of(18, 0);

        LocalTime peakEnd = (tariff != null && tariff.getPeakHourEnd() != null)
                ? tariff.getPeakHourEnd()
                : LocalTime.of(22, 0);

        BigDecimal idleRatePerMin = (tariff != null && tariff.getIdlePenaltyPerMinute() != null)
                ? tariff.getIdlePenaltyPerMinute()
                : new BigDecimal("2.00");

        int gracePeriod = (tariff != null && tariff.getGracePeriodMinutes() != null)
                ? tariff.getGracePeriodMinutes()
                : 15;

        // Energy consumption
        BigDecimal energyKwh = BigDecimal.ZERO;
        if (finalMeter != null && initialMeter != null && finalMeter.compareTo(initialMeter) >= 0) {
            energyKwh = finalMeter.subtract(initialMeter).setScale(3, RoundingMode.HALF_UP);
        }

        // Check if session falls mostly in peak hours
        LocalTime sessionMidTime = startTime != null ? startTime.toLocalTime() : LocalTime.now();
        boolean isPeak = !sessionMidTime.isBefore(peakStart) && sessionMidTime.isBefore(peakEnd);
        BigDecimal effectiveRate = isPeak ? peakRate : baseRate;

        BigDecimal energyCost = energyKwh.multiply(effectiveRate).setScale(2, RoundingMode.HALF_UP);

        // Idle overstay penalty
        BigDecimal idlePenalty = BigDecimal.ZERO;
        if (idleMinutes != null && idleMinutes > gracePeriod) {
            int billableIdleMinutes = idleMinutes - gracePeriod;
            idlePenalty = idleRatePerMin.multiply(BigDecimal.valueOf(billableIdleMinutes))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalCost = energyCost.add(idlePenalty).setScale(2, RoundingMode.HALF_UP);

        return new BillBreakdown(energyKwh, energyCost, idlePenalty, totalCost);
    }
}
