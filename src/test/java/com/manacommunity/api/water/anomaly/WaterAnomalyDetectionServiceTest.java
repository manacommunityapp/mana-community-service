package com.manacommunity.api.water.anomaly;

import com.manacommunity.api.water.anomaly.model.*;
import com.manacommunity.api.water.anomaly.service.WaterAnomalyDetectionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WaterAnomalyDetectionServiceTest {

    private WaterAnomalyDetectionServiceImpl service;
    private final Long communityId = 3001L;

    @BeforeEach
    void setUp() {
        service = new WaterAnomalyDetectionServiceImpl();
    }

    @Test
    @DisplayName("Should approve full payment for verified tanker within tolerance")
    void testVerifiedTankerDelivery() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 27, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 27, 10, 20); // 20 mins decanting

        TankerDeliveryVerificationRequest request = TankerDeliveryVerificationRequest.builder()
                .deliveryId(101L)
                .communityId(communityId)
                .tankerRegistration("KA-01-AB-1234")
                .vendorName("Kavery Water Suppliers")
                .invoicedCapacityLiters(12000.0)
                .invoicedCost(BigDecimal.valueOf(1500.00))
                .targetSumpTankId(5L)
                .decantingStartTime(start)
                .decantingEndTime(end)
                .sumpLevelBeforeLiters(20000.0)
                .sumpLevelAfterLiters(31500.0) // 11,500L rise (4.1% discrepancy, well within 10%)
                .pumpTransfersOutDuringDischargeLiters(0.0)
                .build();

        TankerVerificationResult result = service.verifyTankerDelivery(request);

        assertNotNull(result);
        assertEquals(TankerVerificationResult.Status.VERIFIED, result.getStatus());
        assertEquals(TankerVerificationResult.PaymentAction.APPROVE_FULL_PAYMENT, result.getPaymentRecommendation());
        assertEquals(12000.0, result.getInvoicedLiters());
        assertEquals(11500.0, result.getCalculatedInflowLiters());
        assertEquals(BigDecimal.valueOf(1500.00), result.getApprovedPayableAmount());
    }

    @Test
    @DisplayName("Should detect short delivery and approve pro-rata payment")
    void testShortDeliveryProRataPayment() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 27, 11, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 27, 11, 18);

        TankerDeliveryVerificationRequest request = TankerDeliveryVerificationRequest.builder()
                .deliveryId(102L)
                .communityId(communityId)
                .tankerRegistration("KA-51-C-9988")
                .vendorName("Balaji Water Services")
                .invoicedCapacityLiters(12000.0)
                .invoicedCost(BigDecimal.valueOf(2000.00))
                .decantingStartTime(start)
                .decantingEndTime(end)
                .sumpLevelBeforeLiters(10000.0)
                .sumpLevelAfterLiters(19800.0) // 9,800L delivered (~18.3% shortfall)
                .pumpTransfersOutDuringDischargeLiters(0.0)
                .build();

        TankerVerificationResult result = service.verifyTankerDelivery(request);

        assertNotNull(result);
        assertEquals(TankerVerificationResult.Status.SHORT_DELIVERY_DETECTED, result.getStatus());
        assertEquals(TankerVerificationResult.PaymentAction.PAY_PRO_RATA, result.getPaymentRecommendation());
        assertEquals(9800.0, result.getCalculatedInflowLiters());
        assertEquals(18.33, result.getDiscrepancyPercentage(), 0.1);
        // Pro-rata: 2000 * (9800/12000) = 1633.33
        assertEquals(new BigDecimal("1633.33"), result.getApprovedPayableAmount());
    }

    @Test
    @DisplayName("Should flag ghost tanker billing when shortfall exceeds 25%")
    void testGhostTankerFraudDetection() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 27, 12, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 27, 12, 16);

        TankerDeliveryVerificationRequest request = TankerDeliveryVerificationRequest.builder()
                .deliveryId(103L)
                .communityId(communityId)
                .tankerRegistration("KA-04-E-4422")
                .vendorName("Fake Star Water")
                .invoicedCapacityLiters(12000.0)
                .invoicedCost(BigDecimal.valueOf(1800.00))
                .decantingStartTime(start)
                .decantingEndTime(end)
                .sumpLevelBeforeLiters(15000.0)
                .sumpLevelAfterLiters(20500.0) // only 5,500L entered sump (54.1% missing)
                .pumpTransfersOutDuringDischargeLiters(0.0)
                .build();

        TankerVerificationResult result = service.verifyTankerDelivery(request);

        assertNotNull(result);
        assertEquals(TankerVerificationResult.Status.GHOST_TANKER_SUSPECTED, result.getStatus());
        assertEquals(TankerVerificationResult.PaymentAction.WITHHOLD_PAYMENT_AUDIT, result.getPaymentRecommendation());
        assertEquals(BigDecimal.ZERO, result.getApprovedPayableAmount());
        assertTrue(result.getDiscrepancyPercentage() > 25.0);
    }

    @Test
    @DisplayName("Should flag phantom delivery when zero or negligible water enters sump")
    void testPhantomDeliveryNoWater() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 27, 14, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 27, 14, 15);

        TankerDeliveryVerificationRequest request = TankerDeliveryVerificationRequest.builder()
                .deliveryId(104L)
                .communityId(communityId)
                .tankerRegistration("KA-03-F-1111")
                .invoicedCapacityLiters(12000.0)
                .invoicedCost(BigDecimal.valueOf(1800.00))
                .decantingStartTime(start)
                .decantingEndTime(end)
                .sumpLevelBeforeLiters(10000.0)
                .sumpLevelAfterLiters(10150.0) // 150L rise (< 500L)
                .pumpTransfersOutDuringDischargeLiters(0.0)
                .build();

        TankerVerificationResult result = service.verifyTankerDelivery(request);

        assertNotNull(result);
        assertEquals(TankerVerificationResult.Status.PHANTOM_DELIVERY_NO_WATER, result.getStatus());
        assertEquals(TankerVerificationResult.PaymentAction.WITHHOLD_PAYMENT_AUDIT, result.getPaymentRecommendation());
        assertEquals(BigDecimal.ZERO, result.getApprovedPayableAmount());
    }

    @Test
    @DisplayName("Should detect physically impossible discharge duration")
    void testImpossibleDischargeDuration() {
        // 12,000 Liters claimed discharged in 2.5 minutes
        LocalDateTime start = LocalDateTime.of(2026, 11, 27, 15, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 27, 15, 2, 30);

        TankerDeliveryVerificationRequest request = TankerDeliveryVerificationRequest.builder()
                .deliveryId(105L)
                .communityId(communityId)
                .invoicedCapacityLiters(12000.0)
                .invoicedCost(BigDecimal.valueOf(1800.00))
                .decantingStartTime(start)
                .decantingEndTime(end)
                .sumpLevelBeforeLiters(10000.0)
                .sumpLevelAfterLiters(21500.0)
                .build();

        TankerVerificationResult result = service.verifyTankerDelivery(request);

        assertNotNull(result);
        assertEquals(TankerVerificationResult.Status.IMPOSSIBLE_DISCHARGE_DURATION, result.getStatus());
        assertEquals(TankerVerificationResult.PaymentAction.WITHHOLD_PAYMENT_AUDIT, result.getPaymentRecommendation());
    }

    @Test
    @DisplayName("Should detect critical underground leak from sustained night-time flow")
    void testCriticalUndergroundLeakDetection() {
        LocalDate date = LocalDate.of(2026, 11, 27);
        // 400 flats -> expected baseline 10 LPM (400 * 0.025 LPM)
        // Measured flow during 2 AM - 4:30 AM is 65 LPM (6.5x baseline)
        List<Double> nightReadings = Arrays.asList(68.0, 65.5, 66.0, 67.2, 65.0, 66.8);

        NightFlowLeakAnalysisRequest request = NightFlowLeakAnalysisRequest.builder()
                .communityId(communityId)
                .analysisDate(date)
                .numberOfFlats(400)
                .flowMeterReadingsLpm(nightReadings)
                .pumpTransferActive(false)
                .fireLinePressureDropDetected(false)
                .build();

        NightFlowLeakAnalysisResult result = service.detectNightFlowLeaks(request);

        assertNotNull(result);
        assertTrue(result.isLeakDetected());
        assertEquals(NightFlowLeakAnalysisResult.Severity.CRITICAL_UNDERGROUND_LEAK, result.getSeverity());
        assertEquals(65.0, result.getMinimumNightFlowLpm());
        assertEquals(10.0, result.getExpectedBaselineLpm());
        assertEquals(55.0, result.getEstimatedLeakRateLpm());
        assertEquals(3300.0, result.getEstimatedLeakRateLph()); // 55 * 60 = 3,300 L/hr
        assertEquals(79200.0, result.getDailyProjectedLossLiters()); // 3300 * 24 = 79,200 L/day
        assertTrue(result.getAlertMessage().contains("CRITICAL UNDERGROUND LEAK DETECTED"));
    }

    @Test
    @DisplayName("Should suppress leak false positive when internal pump transfer is verified active")
    void testSuppressLeakDuringActivePumpTransfer() {
        NightFlowLeakAnalysisRequest request = NightFlowLeakAnalysisRequest.builder()
                .communityId(communityId)
                .analysisDate(LocalDate.of(2026, 11, 27))
                .numberOfFlats(400)
                .flowMeterReadingsLpm(Arrays.asList(80.0, 82.0, 79.5))
                .pumpTransferActive(true) // Pump transfer between sumps running
                .build();

        NightFlowLeakAnalysisResult result = service.detectNightFlowLeaks(request);

        assertNotNull(result);
        assertFalse(result.isLeakDetected());
        assertEquals(NightFlowLeakAnalysisResult.Severity.NORMAL, result.getSeverity());
        assertTrue(result.getAlertMessage().contains("verified internal pump transfer"));
    }

    @Test
    @DisplayName("Should calculate society water mass balance and flag excessive unaccounted loss")
    void testSocietyMassBalanceReconciliation() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 26, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 27, 0, 0);

        SocietyMassBalanceRequest request = SocietyMassBalanceRequest.builder()
                .communityId(communityId)
                .periodStart(start)
                .periodEnd(end)
                .initialStorageLiters(100000.0)
                .finalStorageLiters(95000.0) // actual delta = -5000L
                .verifiedTankerInflowLiters(50000.0)
                .borewellInflowLiters(30000.0)
                .municipalInflowLiters(20000.0) // Total Inflow = 100,000L
                .towerDistributionOutflowLiters(70000.0)
                .treatmentLossLiters(5000.0)   // Total Outflow = 75,000L
                // Theoretical Delta = 100,000 - 75,000 = +25,000L
                // Unaccounted = 25,000 - (-5,000) = 30,000L (30% loss)
                .build();

        SocietyMassBalanceResult result = service.calculateMassBalance(request);

        assertNotNull(result);
        assertEquals(100000.0, result.getTotalInflowLiters());
        assertEquals(75000.0, result.getTotalOutflowLiters());
        assertEquals(30000.0, result.getUnaccountedWaterLiters());
        assertEquals(30.0, result.getUnaccountedLossPercentage());
        assertEquals(SocietyMassBalanceResult.AuditStatus.HIGH_UNACCOUNTED_WATER_LOSS, result.getAuditStatus());
        assertTrue(result.getAuditSummary().contains("HIGH UNACCOUNTED WATER LOSS"));
    }
}
