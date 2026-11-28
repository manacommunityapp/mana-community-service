package com.manacommunity.api.unit.service;

import com.manacommunity.api.concurrencydrill.entity.AtomicGateApproval;
import com.manacommunity.api.concurrencydrill.entity.GateApprovalOutboxEvent;
import com.manacommunity.api.concurrencydrill.repository.AtomicErpReceiptRepository;
import com.manacommunity.api.concurrencydrill.repository.AtomicErpReceiptRepository.ReceiptExportRow;
import com.manacommunity.api.concurrencydrill.repository.AtomicGateApprovalRepository;
import com.manacommunity.api.concurrencydrill.repository.GateApprovalOutboxRepository;
import com.manacommunity.api.concurrencydrill.service.AtomicGateApprovalService;
import com.manacommunity.api.concurrencydrill.service.AtomicGateApprovalService.ApprovalConflictException;
import com.manacommunity.api.concurrencydrill.service.KeysetErpExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.StringWriter;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrencyAndStreamingDrillTest {

    @Mock private AtomicGateApprovalRepository approvalRepo;
    @Mock private GateApprovalOutboxRepository outboxRepo;
    @Mock private AtomicErpReceiptRepository receiptRepo;

    private AtomicGateApprovalService approvalService;
    private KeysetErpExportService exportService;

    private final Map<Long, AtomicGateApproval> approvalStore = new ConcurrentHashMap<>();
    private final Map<String, GateApprovalOutboxEvent> outboxStore = new ConcurrentHashMap<>();
    private final AtomicLong outboxSeq = new AtomicLong(1L);

    @BeforeEach
    void setUp() {
        approvalStore.clear();
        outboxStore.clear();

        approvalStore.put(100L, AtomicGateApproval.builder()
                .id(100L).communityId(412L).flatId(501L).visitorName("Ramesh")
                .status("PENDING").version(1).build());

        lenient().when(approvalRepo.findById(anyLong())).thenAnswer(inv ->
                Optional.ofNullable(approvalStore.get(inv.getArgument(0))));

        // Simulate atomic DB CAS guard: only succeeds if current row is PENDING
        lenient().when(approvalRepo.transitionFromPending(anyLong(), anyString(), anyLong(), any()))
                .thenAnswer(inv -> {
                    Long id = inv.getArgument(0);
                    String nextStatus = inv.getArgument(1);
                    Long actorId = inv.getArgument(2);
                    Instant decidedAt = inv.getArgument(3);
                    AtomicGateApproval row = approvalStore.get(id);
                    if (row == null) return 0;
                    synchronized (row) {
                        if (!"PENDING".equals(row.getStatus())) return 0;
                        row.setStatus(nextStatus);
                        row.setDecidedBy(actorId);
                        row.setDecidedAt(decidedAt);
                        row.setVersion(row.getVersion() + 1);
                        return 1;
                    }
                });

        lenient().when(outboxRepo.findByIdempotencyKey(anyString())).thenAnswer(inv ->
                Optional.ofNullable(outboxStore.get(inv.getArgument(0))));
        lenient().when(outboxRepo.save(any())).thenAnswer(inv -> {
            GateApprovalOutboxEvent ev = inv.getArgument(0);
            if (ev.getId() == null) ev.setId(outboxSeq.getAndIncrement());
            outboxStore.put(ev.getIdempotencyKey(), ev);
            return ev;
        });

        approvalService = new AtomicGateApprovalService(approvalRepo, outboxRepo);
        exportService = new KeysetErpExportService(receiptRepo);
    }

    @Test
    void enforcesAtomicCasTransitionTransactionalOutboxAndIdempotentReplay() {
        var first = approvalService.decideApproval(100L, "APPROVED", 88L, "idem-approve-100");
        assertThat(first.status()).isEqualTo("APPROVED");
        assertThat(first.version()).isEqualTo(2);
        assertThat(first.idempotentReplay()).isFalse();
        assertThat(outboxStore).hasSize(1);
        assertThat(outboxStore.get("idem-approve-100").getEventType()).isEqualTo("GATE_APPROVAL_APPROVED");

        // Concurrent opposing DENIED request is rejected with conflict instead of overwriting APPROVED
        assertThatThrownBy(() -> approvalService.decideApproval(100L, "DENIED", 99L, "idem-deny-100"))
                .isInstanceOf(ApprovalConflictException.class)
                .hasMessageContaining("APPROVED");
        assertThat(outboxStore).hasSize(1);

        // Mobile 2G retry with identical idempotencyKey replays settled result without duplicate outbox event
        var replay = approvalService.decideApproval(100L, "APPROVED", 88L, "idem-approve-100");
        assertThat(replay.idempotentReplay()).isTrue();
        assertThat(replay.status()).isEqualTo("APPROVED");
        assertThat(outboxStore).hasSize(1);
    }

    @Test
    void streamsReceiptsViaBoundedKeysetCursorBatchesAndNeutralizesCsvInjection() throws Exception {
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t1 = Instant.parse("2026-12-31T23:59:59Z");

        List<ReceiptExportRow> dataset = List.of(
                new ReceiptExportRow(1L, "A-101", 250000L, "UPI", Instant.parse("2026-02-01T08:00:00Z")),
                new ReceiptExportRow(2L, "A-102", 250000L, "NEFT", Instant.parse("2026-03-01T08:00:00Z")),
                new ReceiptExportRow(3L, "=cmd|'/C calc'!A0", 300000L, "CASH", Instant.parse("2026-04-01T08:00:00Z")),
                new ReceiptExportRow(4L, "B-201", 180000L, "CARD", Instant.parse("2026-05-01T08:00:00Z")),
                new ReceiptExportRow(5L, "B-202,East", 190000L, "UPI", Instant.parse("2026-06-01T08:00:00Z"))
        );

        when(receiptRepo.fetchExportBatch(eq(412L), eq(t0), eq(t1), anyLong(), any())).thenAnswer(inv -> {
            long cursor = inv.getArgument(3);
            int limit = ((org.springframework.data.domain.Pageable) inv.getArgument(4)).getPageSize();
            return dataset.stream().filter(r -> r.receiptId() > cursor).limit(limit).toList();
        });

        StringWriter sw = new StringWriter();
        long exported = exportService.streamReceiptsCsv(412L, t0, t1, sw, 2);

        assertThat(exported).isEqualTo(5L);
        String csv = sw.toString();
        assertThat(csv).contains("receipt_id,flat_no,amount_paise,payment_mode,paid_at");
        assertThat(csv).contains("3,'=cmd|'/C calc'!A0,300000,CASH");
        assertThat(csv).contains("5,\"B-202,East\",190000,UPI");

        // Keyset batches invoked for cursor 0 (2 rows), 2 (2 rows), 4 (1 row -> partial batch terminates loop)
        verify(receiptRepo, times(1)).fetchExportBatch(eq(412L), eq(t0), eq(t1), eq(0L), any());
        verify(receiptRepo, times(1)).fetchExportBatch(eq(412L), eq(t0), eq(t1), eq(2L), any());
        verify(receiptRepo, times(1)).fetchExportBatch(eq(412L), eq(t0), eq(t1), eq(4L), any());
    }
}
