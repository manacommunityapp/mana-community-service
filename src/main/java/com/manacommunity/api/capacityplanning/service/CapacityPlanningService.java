package com.manacommunity.api.capacityplanning.service;

import lombok.Builder;
import org.springframework.stereotype.Service;

/**
 * Deterministic Back-of-the-Envelope Capacity & SLA Sizing Calculator
 * for Peak Morning Gate Rushes and Monthly Overnight Billing Blasts.
 */
@Service
public class CapacityPlanningService {

    @Builder
    public record MorningRushScenario(
            long societies,
            int avgFlatsPerSociety,
            double dailyEntriesPerFlat,
            double morningConcentrationPct,
            double windowHours,
            double burstMultiplier,
            double callsPerEntry,
            double realTimeAlertPct,
            double avgDevicesPerFlat,
            int ttlSeconds,
            int bytesPerSession,
            double photoCapturePct,
            double avgPhotoKb
    ) {}

    @Builder
    public record MorningRushEstimate(
            long totalFlats,
            double dailyEntries,
            double morningEntries,
            double sustainedEntryQps,
            double burstEntryQps,
            double peakApiQps,
            double burstPushFanoutQps,
            long redisActiveSessionsAtBurst,
            double redisMemoryGb,
            double dailyS3GrowthTb,
            double monthlyS3GrowthTb
    ) {}

    @Builder
    public record BillingBlastScenario(
            long totalInvoices,
            double windowHours,
            double slackHours,
            double cpuMsPerPdf,
            double wallMsPerInvoice,
            double targetCpuUtilizationPct,
            double avgPdfKb,
            double batchedWriteOpsPerInvoice
    ) {}

    @Builder
    public record BillingBlastEstimate(
            double effectiveHours,
            double sustainedInvoicesPerSec,
            double targetInvoicesPerSec,
            double estimatedDbWriteIops,
            int requiredVcpus,
            int recommendedWorkerConcurrency,
            int recommendedKafkaPartitions,
            double monthlyPdfStorageTb,
            boolean deferPspApiToClick
    ) {}

    public MorningRushEstimate estimateMorningRush(MorningRushScenario req) {
        if (req.societies() <= 0 || req.avgFlatsPerSociety() <= 0 || req.windowHours() <= 0) {
            throw new IllegalArgumentException("societies, avgFlatsPerSociety, and windowHours must be positive");
        }
        long totalFlats = req.societies() * req.avgFlatsPerSociety();
        double dailyEntries = totalFlats * req.dailyEntriesPerFlat();
        double morningEntries = dailyEntries * (req.morningConcentrationPct() / 100.0);
        double windowSec = req.windowHours() * 3600.0;

        double sustainedEntryQps = morningEntries / windowSec;
        double burstEntryQps = sustainedEntryQps * req.burstMultiplier();
        double peakApiQps = burstEntryQps * req.callsPerEntry();
        double burstPushFanoutQps = burstEntryQps * (req.realTimeAlertPct() / 100.0) * req.avgDevicesPerFlat();

        long redisActiveSessionsAtBurst = Math.round(burstEntryQps * req.ttlSeconds());
        double redisMemoryGb = (redisActiveSessionsAtBurst * (double) req.bytesPerSession()) / (1024.0 * 1024.0 * 1024.0);

        double capturedPhotosPerDay = dailyEntries * (req.photoCapturePct() / 100.0);
        double dailyS3GrowthTb = (capturedPhotosPerDay * req.avgPhotoKb()) / 1_000_000_000.0;
        double monthlyS3GrowthTb = dailyS3GrowthTb * 30.0;

        return MorningRushEstimate.builder()
                .totalFlats(totalFlats)
                .dailyEntries(dailyEntries)
                .morningEntries(morningEntries)
                .sustainedEntryQps(round2(sustainedEntryQps))
                .burstEntryQps(round2(burstEntryQps))
                .peakApiQps(round2(peakApiQps))
                .burstPushFanoutQps(round2(burstPushFanoutQps))
                .redisActiveSessionsAtBurst(redisActiveSessionsAtBurst)
                .redisMemoryGb(round2(redisMemoryGb))
                .dailyS3GrowthTb(round2(dailyS3GrowthTb))
                .monthlyS3GrowthTb(round2(monthlyS3GrowthTb))
                .build();
    }

    public BillingBlastEstimate estimateMonthlyBillingBlast(BillingBlastScenario req) {
        if (req.totalInvoices() <= 0 || req.windowHours() <= 0) {
            throw new IllegalArgumentException("totalInvoices and windowHours must be positive");
        }
        double effectiveHours = Math.max(0.5, req.windowHours() - Math.max(0.0, req.slackHours()));
        double sustainedInvoicesPerSec = req.totalInvoices() / (req.windowHours() * 3600.0);
        double targetInvoicesPerSec = req.totalInvoices() / (effectiveHours * 3600.0);

        double estimatedDbWriteIops = targetInvoicesPerSec * req.batchedWriteOpsPerInvoice();
        double rawCores = targetInvoicesPerSec * (req.cpuMsPerPdf() / 1000.0);
        double utilFraction = Math.max(0.2, Math.min(0.95, req.targetCpuUtilizationPct() / 100.0));
        int requiredVcpus = (int) Math.ceil(rawCores / utilFraction);

        // Little's Law L = lambda * W, with 2.5x tail-latency buffer for S3 jitter
        double littlesLawInFlight = targetInvoicesPerSec * (req.wallMsPerInvoice() / 1000.0);
        int recommendedWorkerConcurrency = (int) Math.ceil(littlesLawInFlight * 2.5);

        int partitions = nextPowerOfTwo(Math.max(16, recommendedWorkerConcurrency));
        double monthlyPdfStorageTb = (req.totalInvoices() * req.avgPdfKb()) / 1_000_000_000.0;

        return BillingBlastEstimate.builder()
                .effectiveHours(round2(effectiveHours))
                .sustainedInvoicesPerSec(round2(sustainedInvoicesPerSec))
                .targetInvoicesPerSec(round2(targetInvoicesPerSec))
                .estimatedDbWriteIops(round2(estimatedDbWriteIops))
                .requiredVcpus(requiredVcpus)
                .recommendedWorkerConcurrency(recommendedWorkerConcurrency)
                .recommendedKafkaPartitions(partitions)
                .monthlyPdfStorageTb(round2(monthlyPdfStorageTb))
                .deferPspApiToClick(true)
                .build();
    }

    private static int nextPowerOfTwo(int n) {
        int p = 1;
        while (p < n) p <<= 1;
        return p;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
