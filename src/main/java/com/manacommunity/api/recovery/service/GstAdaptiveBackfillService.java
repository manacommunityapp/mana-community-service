package com.manacommunity.api.recovery.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.recovery.dto.RecoveryDtos.*;
import com.manacommunity.api.recovery.entity.GstBackfillItemEntity;
import com.manacommunity.api.recovery.entity.GstBackfillJobEntity;
import com.manacommunity.api.recovery.repository.GstBackfillItemRepository;
import com.manacommunity.api.recovery.repository.GstBackfillJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * AIMD (Additive Increase / Multiplicative Decrease) production GST backfill engine
 * generating idempotent corrective debit notes without mutating original tax invoices
 * or overloading PostgreSQL read replicas during peak gate traffic.
 */
@Service
@RequiredArgsConstructor
public class GstAdaptiveBackfillService {

    static final int MIN_BATCH_SIZE = 20;
    static final int MAX_BATCH_SIZE = 500;
    static final int HEALTHY_LAG_MS = 50;
    static final int HIGH_LAG_MS = 200;
    static final int CRITICAL_LAG_MS = 500;

    private final GstBackfillJobRepository jobRepository;
    private final GstBackfillItemRepository itemRepository;

    @Transactional
    public GstBackfillJobDto stageDryRunManifest(StageDryRunRequest req) {
        if (req.incidentRef() == null || req.incidentRef().isBlank()) {
            throw new InvalidInputException("incidentRef is required");
        }
        if (req.candidates() == null || req.candidates().isEmpty()) {
            throw new InvalidInputException("At least one candidate invoice is required for dry-run");
        }

        UUID jobId = UUID.randomUUID();
        Instant now = Instant.now();
        GstBackfillJobEntity job = new GstBackfillJobEntity();
        job.setJobId(jobId);
        job.setIncidentRef(req.incidentRef());
        job.setStatus(BackfillJobStatus.DRY_RUN);
        job.setSettlementMode(req.settlementMode() != null
                ? req.settlementMode()
                : BackfillSettlementMode.ROLL_TO_NEXT_CYCLE);
        job.setCurrentBatchSize(100);
        job.setLastReplicaLagMs(0);
        job.setSleepIntervalMs(20);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);

        Set<Long> societies = new HashSet<>();
        Set<String> flats = new HashSet<>();
        long totalDelta = 0L;
        int seq = 1;

        for (BackfillCandidateInput c : req.candidates()) {
            long delta = c.expectedGstPaise() - c.billedGstPaise();
            if (delta <= 0) {
                continue;
            }
            if (itemRepository.findByJobIdAndOriginalVoucherId(jobId, c.originalVoucherId()).isPresent()) {
                continue;
            }
            GstBackfillItemEntity item = new GstBackfillItemEntity();
            item.setItemId(UUID.randomUUID());
            item.setJobId(jobId);
            item.setSocietyId(c.societyId());
            item.setFlatId(c.flatId());
            item.setUnitCode(c.unitCode() != null ? c.unitCode() : "Flat-" + c.flatId());
            item.setOriginalVoucherId(c.originalVoucherId());
            item.setBilledGstPaise(c.billedGstPaise());
            item.setExpectedGstPaise(c.expectedGstPaise());
            item.setDeltaPaise(delta);
            item.setDebitNoteNumber(String.format("DN-%d-%05d", c.societyId(), seq++));
            item.setIdempotencyKey("dn:" + jobId + ":" + c.originalVoucherId());
            item.setStatus("DRY_RUN");
            item.setCreatedAt(now);
            itemRepository.save(item);

            societies.add(c.societyId());
            flats.add(c.societyId() + ":" + c.flatId());
            totalDelta += delta;
        }

        job.setAffectedSocieties(societies.size());
        job.setAffectedFlats(flats.size());
        job.setTotalDeltaPaise(totalDelta);
        return toJobDto(jobRepository.save(job));
    }

    @Transactional
    public AdaptiveChunkResultDto executeAdaptiveChunk(ExecuteAdaptiveChunkRequest req) {
        GstBackfillJobEntity job = jobRepository.findById(req.jobId())
                .orElseThrow(() -> new ResourceNotFoundException("Backfill job not found: " + req.jobId()));

        if (job.getStatus() == BackfillJobStatus.COMPLETED) {
            return new AdaptiveChunkResultDto(toJobDto(job), 0, "ALREADY_COMPLETED");
        }

        int currentBatch = job.getCurrentBatchSize();
        String aimdAction;
        int nextBatch;
        int nextSleep;
        boolean circuitPause = false;

        if (req.replicaLagMs() >= CRITICAL_LAG_MS || req.lockWaitCount() > 0) {
            nextBatch = Math.max(MIN_BATCH_SIZE, currentBatch / 2);
            nextSleep = 2000;
            job.setStatus(BackfillJobStatus.PAUSED_REPLICA_LAG);
            circuitPause = true;
            aimdAction = "CIRCUIT_PAUSE_MULTIPLICATIVE_DECREASE";
        } else if (req.replicaLagMs() >= HIGH_LAG_MS) {
            nextBatch = Math.max(MIN_BATCH_SIZE, currentBatch / 2);
            nextSleep = 2000;
            job.setStatus(BackfillJobStatus.RUNNING);
            aimdAction = "THROTTLE_HALVE_BATCH";
        } else if (req.replicaLagMs() >= HEALTHY_LAG_MS || req.primaryCpuPct() >= 65) {
            nextBatch = currentBatch;
            nextSleep = 250;
            job.setStatus(BackfillJobStatus.RUNNING);
            aimdAction = "HOLD_STEADY";
        } else {
            nextBatch = Math.min(MAX_BATCH_SIZE, currentBatch + 25);
            nextSleep = 20;
            job.setStatus(BackfillJobStatus.RUNNING);
            aimdAction = "ADDITIVE_INCREASE";
        }

        job.setCurrentBatchSize(nextBatch);
        job.setLastReplicaLagMs(req.replicaLagMs());
        job.setSleepIntervalMs(nextSleep);
        job.setUpdatedAt(Instant.now());

        if (circuitPause) {
            return new AdaptiveChunkResultDto(toJobDto(jobRepository.save(job)), 0, aimdAction);
        }

        List<GstBackfillItemEntity> items = itemRepository.findByJobIdOrderBySocietyIdAscFlatIdAsc(job.getJobId());
        String targetStatus = job.getSettlementMode() == BackfillSettlementMode.ROLL_TO_NEXT_CYCLE
                ? "ROLLED_TO_NEXT_CYCLE"
                : "POSTED";

        int processedNow = 0;
        int remainingDryRun = 0;
        for (GstBackfillItemEntity item : items) {
            if ("DRY_RUN".equals(item.getStatus())) {
                if (processedNow < nextBatch) {
                    item.setStatus(targetStatus);
                    itemRepository.save(item);
                    processedNow++;
                } else {
                    remainingDryRun++;
                }
            }
        }

        job.setProcessedItems(job.getProcessedItems() + processedNow);
        if (remainingDryRun == 0) {
            job.setStatus(BackfillJobStatus.COMPLETED);
            job.setTreasurerNotifiedCount(job.getAffectedSocieties());
        }

        return new AdaptiveChunkResultDto(toJobDto(jobRepository.save(job)), processedNow, aimdAction);
    }

    @Transactional
    public GstBackfillJobDto seedIncidentDryRunDemo(String incidentRef) {
        List<BackfillCandidateInput> demo = new ArrayList<>();
        for (int soc = 1; soc <= 6; soc++) {
            for (int flat = 101; flat <= 110; flat++) {
                demo.add(new BackfillCandidateInput(
                        soc,
                        flat,
                        "T" + soc + "-" + flat,
                        UUID.nameUUIDFromBytes((incidentRef + ":" + soc + ":" + flat).getBytes()),
                        45_000L,
                        54_000L
                ));
            }
        }
        return stageDryRunManifest(new StageDryRunRequest(
                incidentRef != null && !incidentRef.isBlank() ? incidentRef : "INC-GST-UNDERBILL-3D",
                BackfillSettlementMode.ROLL_TO_NEXT_CYCLE,
                demo
        ));
    }

    @Transactional(readOnly = true)
    public List<GstBackfillJobDto> listJobs() {
        return jobRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toJobDto).toList();
    }

    @Transactional(readOnly = true)
    public List<GstBackfillItemDto> listJobItems(UUID jobId) {
        return itemRepository.findByJobIdOrderBySocietyIdAscFlatIdAsc(jobId).stream()
                .map(this::toItemDto)
                .toList();
    }

    private GstBackfillJobDto toJobDto(GstBackfillJobEntity j) {
        return new GstBackfillJobDto(
                j.getJobId(),
                j.getIncidentRef(),
                j.getStatus(),
                j.getSettlementMode(),
                j.getAffectedSocieties(),
                j.getAffectedFlats(),
                j.getProcessedItems(),
                j.getTotalDeltaPaise(),
                j.getCurrentBatchSize(),
                j.getLastReplicaLagMs(),
                j.getSleepIntervalMs(),
                j.getTreasurerNotifiedCount(),
                j.getCreatedAt(),
                j.getUpdatedAt()
        );
    }

    private GstBackfillItemDto toItemDto(GstBackfillItemEntity i) {
        return new GstBackfillItemDto(
                i.getItemId(),
                i.getJobId(),
                i.getSocietyId(),
                i.getFlatId(),
                i.getUnitCode(),
                i.getOriginalVoucherId(),
                i.getBilledGstPaise(),
                i.getExpectedGstPaise(),
                i.getDeltaPaise(),
                i.getDebitNoteNumber(),
                i.getIdempotencyKey(),
                i.getStatus()
        );
    }
}
