package com.manacommunity.api.iot.metering.engine;

import com.manacommunity.api.iot.metering.MeteringEnums.*;
import com.manacommunity.api.iot.metering.SmartMeter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class MeterPulseIngestionEngine {

    // Thresholds for anomaly detection
    private static final BigDecimal MAX_HOURLY_WATER_LITERS = new BigDecimal("2000.0"); // 2000 L/hr leak alert
    private static final BigDecimal MAX_INSTANT_POWER_KW = new BigDecimal("25.0");      // 25 kW surge alert

    public IngestionAnalysis processPulse(SmartMeter meter, Long newPulseCount, BigDecimal instantFlow, Boolean tamperFlag) {
        if (newPulseCount == null || newPulseCount < 0) {
            return new IngestionAnalysis(BigDecimal.ZERO, meter.getLastReading(), true, "Invalid negative pulse count", MeterStatus.FAULT, false);
        }

        BigDecimal multiplier = meter.getPulseMultiplier() != null ? meter.getPulseMultiplier() : BigDecimal.ONE;

        // Rollback / Tamper detection
        if (newPulseCount < meter.getLastPulseCount()) {
            return new IngestionAnalysis(
                    BigDecimal.ZERO,
                    meter.getLastReading(),
                    true,
                    "Meter rollback detected! Possible hardware tamper or reset.",
                    MeterStatus.TAMPERED,
                    true
            );
        }

        if (Boolean.TRUE.equals(tamperFlag)) {
            return new IngestionAnalysis(
                    BigDecimal.ZERO,
                    meter.getLastReading(),
                    true,
                    "Hardware tamper switch activated on meter",
                    MeterStatus.TAMPERED,
                    true
            );
        }

        long deltaPulses = newPulseCount - meter.getLastPulseCount();
        BigDecimal deltaUnits = BigDecimal.valueOf(deltaPulses)
                .multiply(multiplier)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal cumulative = meter.getLastReading().add(deltaUnits).setScale(4, RoundingMode.HALF_UP);

        boolean leakOrBurst = false;
        String alert = null;

        if (meter.getMeterType() == MeterType.WATER_METER && deltaUnits.compareTo(MAX_HOURLY_WATER_LITERS) > 0) {
            leakOrBurst = true;
            alert = "Excessive water flow anomaly detected (> " + MAX_HOURLY_WATER_LITERS + " L)";
        } else if (meter.getMeterType() == MeterType.ELECTRICITY_METER && instantFlow != null && instantFlow.compareTo(MAX_INSTANT_POWER_KW) > 0) {
            alert = "Power draw surge spike (> " + MAX_INSTANT_POWER_KW + " kW)";
        }

        return new IngestionAnalysis(deltaUnits, cumulative, leakOrBurst || alert != null, alert, MeterStatus.ACTIVE, leakOrBurst);
    }

    public record IngestionAnalysis(
            BigDecimal deltaConsumed,
            BigDecimal newCumulativeReading,
            boolean anomalyDetected,
            String alertMessage,
            MeterStatus status,
            boolean leakFlag
    ) {}
}
