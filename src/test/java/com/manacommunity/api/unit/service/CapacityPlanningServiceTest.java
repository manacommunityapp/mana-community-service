package com.manacommunity.api.unit.service;

import com.manacommunity.api.capacityplanning.service.CapacityPlanningService;
import com.manacommunity.api.capacityplanning.service.CapacityPlanningService.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CapacityPlanningServiceTest {

    private final CapacityPlanningService service = new CapacityPlanningService();

    @Test
    void sizesPeakMorningRushAcross30kSocietiesWithRedisTtlAndS3Growth() {
        MorningRushEstimate est = service.estimateMorningRush(MorningRushScenario.builder()
                .societies(30_000L)
                .avgFlatsPerSociety(350)
                .dailyEntriesPerFlat(4.0)
                .morningConcentrationPct(40.0)
                .windowHours(2.0)
                .burstMultiplier(3.5)
                .callsPerEntry(3.0)
                .realTimeAlertPct(60.0)
                .avgDevicesPerFlat(2.0)
                .ttlSeconds(300)
                .bytesPerSession(1024)
                .photoCapturePct(50.0)
                .avgPhotoKb(150.0)
                .build());

        assertThat(est.totalFlats()).isEqualTo(10_500_000L);
        assertThat(est.dailyEntries()).isEqualTo(42_000_000.0);
        assertThat(est.morningEntries()).isEqualTo(16_800_000.0);
        assertThat(est.sustainedEntryQps()).isBetween(2330.0, 2335.0);
        assertThat(est.burstEntryQps()).isGreaterThan(8000.0);
        assertThat(est.peakApiQps()).isGreaterThan(24_000.0);
        assertThat(est.burstPushFanoutQps()).isGreaterThan(9_500.0);
        assertThat(est.redisActiveSessionsAtBurst()).isGreaterThan(2_400_000L);
        assertThat(est.redisMemoryGb()).isBetween(2.2, 2.5);
        assertThat(est.dailyS3GrowthTb()).isEqualTo(3.15);
        assertThat(est.monthlyS3GrowthTb()).isEqualTo(94.5);
    }

    @Test
    void sizesMonthlyBillingBlastFor10MillionInvoicesIn4HourOvernightWindow() {
        BillingBlastEstimate est = service.estimateMonthlyBillingBlast(BillingBlastScenario.builder()
                .totalInvoices(10_000_000L)
                .windowHours(4.0)
                .slackHours(1.0) // 3h effective target with 1h retry slack
                .cpuMsPerPdf(25.0)
                .wallMsPerInvoice(50.0)
                .targetCpuUtilizationPct(60.0)
                .avgPdfKb(120.0)
                .batchedWriteOpsPerInvoice(8.0)
                .build());

        assertThat(est.effectiveHours()).isEqualTo(3.0);
        assertThat(est.sustainedInvoicesPerSec()).isBetween(694.0, 695.0);
        assertThat(est.targetInvoicesPerSec()).isBetween(925.0, 926.5);
        assertThat(est.requiredVcpus()).isBetween(38, 42);
        assertThat(est.recommendedWorkerConcurrency()).isGreaterThanOrEqualTo(115);
        assertThat(est.recommendedKafkaPartitions()).isEqualTo(128);
        assertThat(est.estimatedDbWriteIops()).isGreaterThan(7_000.0);
        assertThat(est.monthlyPdfStorageTb()).isEqualTo(1.2);
        assertThat(est.deferPspApiToClick()).isTrue();
    }
}
