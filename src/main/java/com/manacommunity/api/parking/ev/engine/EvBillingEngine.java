package com.manacommunity.api.parking.ev.engine;

import com.manacommunity.api.parking.ev.entity.EvChargingRate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Component
public class EvBillingEngine {

    /**
     * Calculates the net energy consumed in kWh from meter readings.
     */
    public double calculateKwh(Double startMeter, Double endMeter) {
        if (startMeter == null || endMeter == null) return 0.0;
        if (endMeter < startMeter) return 0.0; // protects against rollover/faulty telemetry
        double delta = endMeter - startMeter;
        return Math.round(delta * 100.0) / 100.0;
    }

    /**
     * Calculates the total charging cost based on energy consumed, time of use (peak/off-peak),
     * and minimum billing thresholds.
     */
    public BigDecimal calculateCost(double totalKwh, LocalDateTime startedAt, LocalDateTime endedAt, EvChargingRate rateConfig) {
        if (rateConfig == null) {
            BigDecimal defaultRate = new BigDecimal("8.50");
            return BigDecimal.valueOf(totalKwh).multiply(defaultRate).setScale(2, RoundingMode.HALF_UP);
        }

        if (totalKwh <= 0.0) {
            return rateConfig.getMinimumBillAmount() != null ? rateConfig.getMinimumBillAmount() : BigDecimal.ZERO;
        }

        boolean isPeak = false;
        if (startedAt != null) {
            int startHour = startedAt.getHour();
            isPeak = startHour >= rateConfig.getPeakStartHour() && startHour < rateConfig.getPeakEndHour();
        }

        BigDecimal effectiveRate = isPeak ? rateConfig.getPeakRatePerKwh() : rateConfig.getUnitRatePerKwh();
        BigDecimal energyCost = BigDecimal.valueOf(totalKwh).multiply(effectiveRate).setScale(2, RoundingMode.HALF_UP);

        if (rateConfig.getMinimumBillAmount() != null && energyCost.compareTo(rateConfig.getMinimumBillAmount()) < 0) {
            return rateConfig.getMinimumBillAmount();
        }

        return energyCost;
    }
}