package com.manacommunity.api.forensics.service;

import com.manacommunity.api.forensics.dto.ForensicsDtos.*;
import com.manacommunity.api.forensics.entity.BitemporalLedgerEventEntity;
import com.manacommunity.api.forensics.entity.LedgerCheckpointEntity;
import com.manacommunity.api.forensics.repository.BitemporalLedgerEventRepository;
import com.manacommunity.api.forensics.repository.LedgerCheckpointRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class BitemporalLedgerServiceTest {

    private BitemporalLedgerService createService(
            List<BitemporalLedgerEventEntity> events,
            List<LedgerCheckpointEntity> checkpoints) {
        BitemporalLedgerEventRepository eventRepo = Mockito.mock(BitemporalLedgerEventRepository.class);
        LedgerCheckpointRepository cpRepo = Mockito.mock(LedgerCheckpointRepository.class);

        when(eventRepo.findBySocietyIdAndIdempotencyKey(anyLong(), anyString())).thenAnswer(inv -> {
            long sid = inv.getArgument(0);
            String key = inv.getArgument(1);
            return events.stream()
                    .filter(e -> e.getSocietyId() == sid && key.equals(e.getIdempotencyKey()))
                    .findFirst();
        });
        when(eventRepo.findTopBySocietyIdAndFlatIdOrderByStreamVersionDesc(anyLong(), anyLong())).thenAnswer(inv -> {
            long sid = inv.getArgument(0);
            long fid = inv.getArgument(1);
            return events.stream()
                    .filter(e -> e.getSocietyId() == sid && e.getFlatId() == fid)
                    .max(Comparator.comparingLong(BitemporalLedgerEventEntity::getStreamVersion));
        });
        when(eventRepo.findTopBySocietyIdOrderByGlobalSeqDesc(anyLong())).thenAnswer(inv -> {
            long sid = inv.getArgument(0);
            return events.stream()
                    .filter(e -> e.getSocietyId() == sid)
                    .max(Comparator.comparingLong(BitemporalLedgerEventEntity::getGlobalSeq));
        });
        when(eventRepo.findBySocietyIdAndFlatIdOrderByStreamVersionAsc(anyLong(), anyLong())).thenAnswer(inv -> {
            long sid = inv.getArgument(0);
            long fid = inv.getArgument(1);
            return events.stream()
                    .filter(e -> e.getSocietyId() == sid && e.getFlatId() == fid)
                    .sorted(Comparator.comparingLong(BitemporalLedgerEventEntity::getStreamVersion))
                    .toList();
        });
        when(eventRepo.save(any())).thenAnswer(inv -> {
            BitemporalLedgerEventEntity e = inv.getArgument(0);
            events.add(e);
            return e;
        });

        when(cpRepo.save(any())).thenAnswer(inv -> {
            LedgerCheckpointEntity c = inv.getArgument(0);
            checkpoints.add(c);
            return c;
        });
        when(cpRepo.findTopBySocietyIdAndFlatIdAndRecordedCutoffLessThanEqualOrderByStreamVersionDesc(
                anyLong(), anyLong(), any(Instant.class)))
                .thenAnswer(inv -> {
                    long sid = inv.getArgument(0);
                    long fid = inv.getArgument(1);
                    Instant cutoff = inv.getArgument(2);
                    return checkpoints.stream()
                            .filter(c -> c.getSocietyId() == sid && c.getFlatId() == fid && !c.getRecordedCutoff().isAfter(cutoff))
                            .max(Comparator.comparingLong(LedgerCheckpointEntity::getStreamVersion));
                });

        return new BitemporalLedgerService(eventRepo, cpRepo);
    }

    @Test
    void reconstructsFlatB902BalanceBeforeAndAfterAprilBackdatedCreditNotesAndBouncedCheques() {
        List<BitemporalLedgerEventEntity> events = new ArrayList<>();
        List<LedgerCheckpointEntity> checkpoints = new ArrayList<>();
        BitemporalLedgerService service = createService(events, checkpoints);

        TimeTravelReconstructionDto mar31 = service.seedFlatB902Scenario(1L);

        // Before April entries: ₹8,500 invoice - ₹4,000 chq1 - ₹2,500 chq2 = ₹2,000
        assertEquals(200_000L, mar31.asKnownBalancePaise());
        // Restated March 31 after 2 bounced cheques (+₹6,500) & 4 backdated credit notes (-₹2,000) = ₹6,500
        assertEquals(650_000L, mar31.restatedBalancePaise());
        assertEquals(450_000L, mar31.retroactiveVariancePaise());
        // Live balance includes April 1 invoice (+₹8,500) = ₹15,000
        assertEquals(1_500_000L, mar31.liveCurrentBalancePaise());
        // Isolates exact 6 April retroactive events (2 NSF + 4 Credit Notes)
        assertEquals(6, mar31.retroactiveAdjustments().size());
        // Checkpoint from Feb 28 used -> only replayed the 3 March events!
        assertNotNull(mar31.checkpointUsed());
        assertEquals(3, mar31.eventsReplayedFromCheckpoint());
    }

    @Test
    void deduplicatesIdempotentAppendsAndChainsHashes() {
        List<BitemporalLedgerEventEntity> events = new ArrayList<>();
        List<LedgerCheckpointEntity> checkpoints = new ArrayList<>();
        BitemporalLedgerService service = createService(events, checkpoints);

        AppendLedgerEventRequest req = new AppendLedgerEventRequest(
                1L, 902L, "Tower B - 902", LedgerEventType.MAINTENANCE_INVOICE,
                LocalDate.of(2026, 3, 1), Instant.parse("2026-03-01T04:00:00Z"),
                850_000L, null, "MAR_INV", "BILLING", "idem-key-1", null
        );
        BitemporalLedgerEventDto first = service.appendEvent(req);
        BitemporalLedgerEventDto retry = service.appendEvent(req);

        assertEquals(first.eventId(), retry.eventId());
        assertEquals(1, events.size());
        assertEquals(ForensicsCryptoUtil.GENESIS_HASH, first.prevEventHash());
    }
}
