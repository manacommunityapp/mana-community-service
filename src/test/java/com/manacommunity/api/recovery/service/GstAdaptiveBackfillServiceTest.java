package com.manacommunity.api.recovery.service;

import com.manacommunity.api.recovery.dto.RecoveryDtos.*;
import com.manacommunity.api.recovery.entity.GstBackfillItemEntity;
import com.manacommunity.api.recovery.entity.GstBackfillJobEntity;
import com.manacommunity.api.recovery.repository.GstBackfillItemRepository;
import com.manacommunity.api.recovery.repository.GstBackfillJobRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class GstAdaptiveBackfillServiceTest {

    private GstAdaptiveBackfillService createService(
            Map<UUID, GstBackfillJobEntity> jobs,
            List<GstBackfillItemEntity> items) {
        GstBackfillJobRepository jobRepo = Mockito.mock(GstBackfillJobRepository.class);
        GstBackfillItemRepository itemRepo = Mockito.mock(GstBackfillItemRepository.class);

        when(jobRepo.save(any())).thenAnswer(inv -> {
            GstBackfillJobEntity j = inv.getArgument(0);
            jobs.put(j.getJobId(), j);
            return j;
        });
        when(jobRepo.findById(any())).thenAnswer(inv -> Optional.ofNullable(jobs.get(inv.getArgument(0))));

        when(itemRepo.findByJobIdAndOriginalVoucherId(any(), any())).thenAnswer(inv -> {
            UUID jid = inv.getArgument(0);
            UUID vid = inv.getArgument(1);
            return items.stream()
                    .filter(i -> jid.equals(i.getJobId()) && vid.equals(i.getOriginalVoucherId()))
                    .findFirst();
        });
        when(itemRepo.findByJobIdOrderBySocietyIdAscFlatIdAsc(any())).thenAnswer(inv -> {
            UUID jid = inv.getArgument(0);
            return items.stream().filter(i -> jid.equals(i.getJobId())).toList();
        });
        when(itemRepo.save(any())).thenAnswer(inv -> {
            GstBackfillItemEntity i = inv.getArgument(0);
            items.removeIf(existing -> existing.getItemId().equals(i.getItemId()));
            items.add(i);
            return i;
        });

        return new GstAdaptiveBackfillService(jobRepo, itemRepo);
    }

    @Test
    void pausesOnHighReplicaLagThenCompletesIdempotentlyOnHealthyReplica() {
        Map<UUID, GstBackfillJobEntity> jobs = new HashMap<>();
        List<GstBackfillItemEntity> items = new ArrayList<>();
        GstAdaptiveBackfillService service = createService(jobs, items);

        UUID dupVoucher = UUID.randomUUID();
        List<BackfillCandidateInput> candidates = new ArrayList<>();
        candidates.add(new BackfillCandidateInput(1L, 101L, "A-101", dupVoucher, 45_000L, 54_000L));
        candidates.add(new BackfillCandidateInput(1L, 101L, "A-101", dupVoucher, 45_000L, 54_000L)); // duplicate
        for (int i = 2; i <= 80; i++) {
            candidates.add(new BackfillCandidateInput(
                    (i % 3) + 1L, 100L + i, "T-" + i, UUID.randomUUID(), 45_000L, 54_000L
            ));
        }

        GstBackfillJobDto staged = service.stageDryRunManifest(
                new StageDryRunRequest("INC-GST-3D", BackfillSettlementMode.ROLL_TO_NEXT_CYCLE, candidates)
        );
        assertEquals(BackfillJobStatus.DRY_RUN, staged.status());
        assertEquals(80, staged.affectedFlats());
        assertEquals(3, staged.affectedSocieties());
        assertEquals(720_000L, staged.totalDeltaPaise());

        // Replication lag spike >= 500ms must circuit-pause and process 0 rows
        AdaptiveChunkResultDto paused = service.executeAdaptiveChunk(
                new ExecuteAdaptiveChunkRequest(staged.jobId(), 620, 48, 0)
        );
        assertEquals(0, paused.itemsProcessedInChunk());
        assertEquals(BackfillJobStatus.PAUSED_REPLICA_LAG, paused.job().status());
        assertEquals(50, paused.job().currentBatchSize()); // halved from 100
        assertEquals(2000, paused.job().sleepIntervalMs());

        // Healthy lag (< 50ms) scales batch up (+25 -> 75)
        AdaptiveChunkResultDto chunk1 = service.executeAdaptiveChunk(
                new ExecuteAdaptiveChunkRequest(staged.jobId(), 18, 35, 0)
        );
        assertEquals(75, chunk1.itemsProcessedInChunk());
        assertEquals("ADDITIVE_INCREASE", chunk1.aimdAction());
        assertEquals(BackfillJobStatus.RUNNING, chunk1.job().status());

        // Second healthy chunk finishes remaining 5 and notifies treasurers
        AdaptiveChunkResultDto chunk2 = service.executeAdaptiveChunk(
                new ExecuteAdaptiveChunkRequest(staged.jobId(), 15, 30, 0)
        );
        assertEquals(5, chunk2.itemsProcessedInChunk());
        assertEquals(BackfillJobStatus.COMPLETED, chunk2.job().status());
        assertEquals(80, chunk2.job().processedItems());
        assertEquals(3, chunk2.job().treasurerNotifiedCount());
    }
}
