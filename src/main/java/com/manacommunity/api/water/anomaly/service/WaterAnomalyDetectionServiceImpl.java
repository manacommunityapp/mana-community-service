package com.manacommunity.api.water.anomaly.service;

import com.manacommunity.api.water.anomaly.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class WaterAnomalyDetectionServiceImpl implements WaterAnomalyDetectionService {

    // Maximum theoretical gravity flow rate through 3-inch hose ~ 900 Liters/min
    private static final double MAX_DISCHARGE_LPM = 1200.0;
    // Minimum expected residential night flow per flat: 1.5 Liters/hour = 0.025 Liters/min
    private static final double NIGHT_BASELINE_LPM_PER_FLAT = 0.025;

    @Override
    public TankerVerificationResult verifyTankerDelivery(TankerDeliveryVerificationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Verification request cannot be null");
        }

        double invoicedLiters = request.getInvoicedCapacityLiters();
        BigDecimal invoicedCost = request.getInvoicedCost() != null ? request.getInvoicedCost() : BigDecimal.ZERO;

        // 1. Calculate duration
        double durationMinutes = 0.0;
        if (request.getDecantingStartTime() != null && request.getDecantingEndTime() != null) {
            long seconds = Duration.between(request.getDecantingStartTime(), request.getDecantingEndTime()).getSeconds();
            durationMinutes = Math.max(0.0, round(seconds / 60.0, 2));
        }

        // 2. Calculate actual inflow received
        double calculatedInflow;
        if (request.getFlowMeterTotalizerLiters() != null && request.getFlowMeterTotalizerLiters() > 0) {
            calculatedInflow = request.getFlowMeterTotalizerLiters();
        } else {
            double sumpRise = Math.max(0.0, request.getSumpLevelAfterLiters() - request.getSumpLevelBeforeLiters());
            calculatedInflow = sumpRise + request.getPumpTransfersOutDuringDischargeLiters();
        }
        calculatedInflow = round(calculatedInflow, 2);

        // 3. Check for phantom delivery (less than 500 Liters received for a multi-thousand liter tanker)
        if (calculatedInflow < 500.0 && invoicedLiters >= 5000.0) {
            return TankerVerificationResult.builder()
                    .deliveryId(request.getDeliveryId())
                    .status(TankerVerificationResult.Status.PHANTOM_DELIVERY_NO_WATER)
                    .paymentRecommendation(TankerVerificationResult.PaymentAction.WITHHOLD_PAYMENT_AUDIT)
                    .invoicedLiters(invoicedLiters)
                    .calculatedInflowLiters(calculatedInflow)
                    .discrepancyLiters(invoicedLiters - calculatedInflow)
                    .discrepancyPercentage(100.0)
                    .decantingDurationMinutes(durationMinutes)
                    .invoicedCost(invoicedCost)
                    .approvedPayableAmount(BigDecimal.ZERO)
                    .anomalyReason("Phantom Delivery: Zero or negligible water received (< 500L) despite declared capacity of " + invoicedLiters + "L.")
                    .build();
        }

        // 4. Physical discharge duration check
        double minPhysicallyPossibleMinutes = invoicedLiters / MAX_DISCHARGE_LPM;
        if (durationMinutes > 0 && durationMinutes < (minPhysicallyPossibleMinutes * 0.75)) {
            return TankerVerificationResult.builder()
                    .deliveryId(request.getDeliveryId())
                    .status(TankerVerificationResult.Status.IMPOSSIBLE_DISCHARGE_DURATION)
                    .paymentRecommendation(TankerVerificationResult.PaymentAction.WITHHOLD_PAYMENT_AUDIT)
                    .invoicedLiters(invoicedLiters)
                    .calculatedInflowLiters(calculatedInflow)
                    .discrepancyLiters(invoicedLiters - calculatedInflow)
                    .discrepancyPercentage(round(((invoicedLiters - calculatedInflow) / invoicedLiters) * 100.0, 2))
                    .decantingDurationMinutes(durationMinutes)
                    .invoicedCost(invoicedCost)
                    .approvedPayableAmount(BigDecimal.ZERO)
                    .anomalyReason("Impossible Discharge Duration: " + durationMinutes + " mins recorded for " + invoicedLiters +
                            "L. Physically impossible without high-pressure industrial evacuation (min " + round(minPhysicallyPossibleMinutes, 1) + " mins).")
                    .build();
        }

        // 5. Volume Discrepancy analysis
        double discrepancyLiters = Math.max(0.0, invoicedLiters - calculatedInflow);
        double discrepancyPct = invoicedLiters > 0 ? round((discrepancyLiters / invoicedLiters) * 100.0, 2) : 0.0;

        if (discrepancyPct > 25.0) {
            // Severe shortfall -> Ghost Tanker suspected
            return TankerVerificationResult.builder()
                    .deliveryId(request.getDeliveryId())
                    .status(TankerVerificationResult.Status.GHOST_TANKER_SUSPECTED)
                    .paymentRecommendation(TankerVerificationResult.PaymentAction.WITHHOLD_PAYMENT_AUDIT)
                    .invoicedLiters(invoicedLiters)
                    .calculatedInflowLiters(calculatedInflow)
                    .discrepancyLiters(discrepancyLiters)
                    .discrepancyPercentage(discrepancyPct)
                    .decantingDurationMinutes(durationMinutes)
                    .invoicedCost(invoicedCost)
                    .approvedPayableAmount(BigDecimal.ZERO)
                    .anomalyReason("Ghost Tanker Suspected: Missing " + discrepancyLiters + "L (" + discrepancyPct + "% volume discrepancy). Tanker decanted partial volume.")
                    .build();
        } else if (discrepancyPct > 10.0) {
            // Short delivery -> Pro-rata payment
            double proRataFactor = calculatedInflow / invoicedLiters;
            BigDecimal payable = invoicedCost.multiply(BigDecimal.valueOf(proRataFactor)).setScale(2, RoundingMode.HALF_UP);

            return TankerVerificationResult.builder()
                    .deliveryId(request.getDeliveryId())
                    .status(TankerVerificationResult.Status.SHORT_DELIVERY_DETECTED)
                    .paymentRecommendation(TankerVerificationResult.PaymentAction.PAY_PRO_RATA)
                    .invoicedLiters(invoicedLiters)
                    .calculatedInflowLiters(calculatedInflow)
                    .discrepancyLiters(discrepancyLiters)
                    .discrepancyPercentage(discrepancyPct)
                    .decantingDurationMinutes(durationMinutes)
                    .invoicedCost(invoicedCost)
                    .approvedPayableAmount(payable)
                    .anomalyReason("Short Delivery: " + discrepancyPct + "% shortfall (" + discrepancyLiters + "L missing). Approved pro-rata payment.")
                    .build();
        }

        // Verified within 10% acceptable tolerance (evaporation, residue, sensor precision)
        return TankerVerificationResult.builder()
                .deliveryId(request.getDeliveryId())
                .status(TankerVerificationResult.Status.VERIFIED)
                .paymentRecommendation(TankerVerificationResult.PaymentAction.APPROVE_FULL_PAYMENT)
                .invoicedLiters(invoicedLiters)
                .calculatedInflowLiters(calculatedInflow)
                .discrepancyLiters(discrepancyLiters)
                .discrepancyPercentage(discrepancyPct)
                .decantingDurationMinutes(durationMinutes)
                .invoicedCost(invoicedCost)
                .approvedPayableAmount(invoicedCost)
                .anomalyReason("Verified: Tanker delivered full volume within standard sensor tolerance.")
                .build();
    }

    @Override
    public NightFlowLeakAnalysisResult detectNightFlowLeaks(NightFlowLeakAnalysisRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Night flow request cannot be null");
        }

        int flats = Math.max(1, request.getNumberOfFlats());
        double baselineLpm = round(flats * NIGHT_BASELINE_LPM_PER_FLAT, 2);

        List<Double> readings = request.getFlowMeterReadingsLpm();
        if (readings == null || readings.isEmpty()) {
            return NightFlowLeakAnalysisResult.builder()
                    .communityId(request.getCommunityId())
                    .analysisDate(request.getAnalysisDate())
                    .leakDetected(false)
                    .severity(NightFlowLeakAnalysisResult.Severity.NORMAL)
                    .minimumNightFlowLpm(0.0)
                    .expectedBaselineLpm(baselineLpm)
                    .estimatedLeakRateLpm(0.0)
                    .estimatedLeakRateLph(0.0)
                    .dailyProjectedLossLiters(0.0)
                    .alertMessage("No flow readings recorded in night analysis window.")
                    .build();
        }

        // Minimum sustained night flow (MNF)
        double minNightFlow = readings.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        minNightFlow = round(minNightFlow, 2);

        // If pump transfer was actively running or fire hydrants operated, suppress false positives
        if (request.isPumpTransferActive()) {
            return NightFlowLeakAnalysisResult.builder()
                    .communityId(request.getCommunityId())
                    .analysisDate(request.getAnalysisDate())
                    .leakDetected(false)
                    .severity(NightFlowLeakAnalysisResult.Severity.NORMAL)
                    .minimumNightFlowLpm(minNightFlow)
                    .expectedBaselineLpm(baselineLpm)
                    .estimatedLeakRateLpm(0.0)
                    .estimatedLeakRateLph(0.0)
                    .dailyProjectedLossLiters(0.0)
                    .alertMessage("Night flow elevated due to verified internal pump transfer between raw and treated sumps.")
                    .build();
        }

        double excessLpm = Math.max(0.0, minNightFlow - baselineLpm);
        double excessLph = round(excessLpm * 60.0, 2);
        double dailyProjectedLoss = round(excessLph * 24.0, 2);

        boolean leakDetected = excessLpm > (baselineLpm * 2.0); // Minimum night flow > 3x expected baseline

        NightFlowLeakAnalysisResult.Severity severity = NightFlowLeakAnalysisResult.Severity.NORMAL;
        String alertMsg = "Night flow is normal within expected residential baseline.";

        if (leakDetected) {
            if (minNightFlow >= (baselineLpm * 5.0) || request.isFireLinePressureDropDetected()) {
                severity = NightFlowLeakAnalysisResult.Severity.CRITICAL_UNDERGROUND_LEAK;
                alertMsg = "CRITICAL UNDERGROUND LEAK DETECTED: Continuous night baseline of " + minNightFlow +
                        " LPM exceeds residential threshold (" + baselineLpm + " LPM) by " + round(minNightFlow / baselineLpm, 1) +
                        "x. Estimated water loss: " + excessLph + " L/hour (" + dailyProjectedLoss + " L/day). Inspect main lines immediately.";
            } else {
                severity = NightFlowLeakAnalysisResult.Severity.POSSIBLE_MINOR_LEAK;
                alertMsg = "POSSIBLE MINOR LEAK: Night flow is moderately elevated (" + minNightFlow + " LPM vs baseline " + baselineLpm + " LPM).";
            }
        }

        return NightFlowLeakAnalysisResult.builder()
                .communityId(request.getCommunityId())
                .analysisDate(request.getAnalysisDate())
                .leakDetected(leakDetected)
                .severity(severity)
                .minimumNightFlowLpm(minNightFlow)
                .expectedBaselineLpm(baselineLpm)
                .estimatedLeakRateLpm(round(excessLpm, 2))
                .estimatedLeakRateLph(excessLph)
                .dailyProjectedLossLiters(dailyProjectedLoss)
                .alertMessage(alertMsg)
                .build();
    }

    @Override
    public SocietyMassBalanceResult calculateMassBalance(SocietyMassBalanceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Mass balance request cannot be null");
        }

        double totalInflow = request.getVerifiedTankerInflowLiters() +
                request.getBorewellInflowLiters() +
                request.getMunicipalInflowLiters();

        double totalOutflow = request.getTowerDistributionOutflowLiters() +
                request.getTreatmentLossLiters();

        double actualDelta = request.getFinalStorageLiters() - request.getInitialStorageLiters();
        double theoreticalDelta = totalInflow - totalOutflow;

        // Unaccounted water = Theoretical storage rise - Actual storage rise
        double unaccountedWater = Math.max(0.0, theoreticalDelta - actualDelta);
        double lossPct = totalInflow > 0 ? round((unaccountedWater / totalInflow) * 100.0, 2) : 0.0;

        SocietyMassBalanceResult.AuditStatus status;
        String summary;

        if (lossPct <= 5.0) {
            status = SocietyMassBalanceResult.AuditStatus.BALANCED;
            summary = "Mass balance reconciled: Total inflow " + round(totalInflow, 0) + "L matches consumption within standard 5% operational threshold.";
        } else if (lossPct <= 12.0) {
            status = SocietyMassBalanceResult.AuditStatus.ACCEPTABLE_LOSS;
            summary = "Minor operational variance: " + lossPct + "% (" + round(unaccountedWater, 0) + "L) attributed to backwash and evaporative cooling.";
        } else {
            status = SocietyMassBalanceResult.AuditStatus.HIGH_UNACCOUNTED_WATER_LOSS;
            summary = "HIGH UNACCOUNTED WATER LOSS: " + lossPct + "% (" + round(unaccountedWater, 0) + "L) missing between inflows and distribution meters. Potential distribution line leakage or unmetered extraction.";
        }

        return SocietyMassBalanceResult.builder()
                .communityId(request.getCommunityId())
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .totalInflowLiters(round(totalInflow, 2))
                .totalOutflowLiters(round(totalOutflow, 2))
                .actualStorageDeltaLiters(round(actualDelta, 2))
                .theoreticalStorageDeltaLiters(round(theoreticalDelta, 2))
                .unaccountedWaterLiters(round(unaccountedWater, 2))
                .unaccountedLossPercentage(lossPct)
                .auditStatus(status)
                .auditSummary(summary)
                .build();
    }

    private double round(double val, int places) {
        if (Double.isNaN(val) || Double.isInfinite(val)) return 0.0;
        return BigDecimal.valueOf(val).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }
}
