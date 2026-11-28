package com.manacommunity.api.clubhouse.service;

import com.manacommunity.api.clubhouse.dto.ClubhouseDtos.*;
import com.manacommunity.api.clubhouse.entity.*;
import com.manacommunity.api.clubhouse.repository.*;
import com.manacommunity.api.exception.UnauthorizedActionException;
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

class ClubhousePosServiceTest {

    private ClubhousePosService createServiceWithInMemoryStores(
            Map<Long, FlatTabAccountEntity> accounts,
            Map<UUID, PosTabChallengeEntity> challenges,
            List<PosOrderEntity> orders,
            List<PosDailyBatchEntity> batches) {

        FlatTabAccountRepository accRepo = Mockito.mock(FlatTabAccountRepository.class);
        PosTabChallengeRepository chRepo = Mockito.mock(PosTabChallengeRepository.class);
        PosOrderRepository ordRepo = Mockito.mock(PosOrderRepository.class);
        PosDailyBatchRepository batchRepo = Mockito.mock(PosDailyBatchRepository.class);

        when(accRepo.findBySocietyIdAndFlatId(anyLong(), anyLong())).thenAnswer(inv -> {
            long fid = inv.getArgument(1);
            return Optional.ofNullable(accounts.get(fid));
        });
        when(accRepo.save(any())).thenAnswer(inv -> {
            FlatTabAccountEntity e = inv.getArgument(0);
            accounts.put(e.getFlatId(), e);
            return e;
        });

        when(chRepo.save(any())).thenAnswer(inv -> {
            PosTabChallengeEntity c = inv.getArgument(0);
            challenges.put(c.getChallengeId(), c);
            return c;
        });
        when(chRepo.findById(any())).thenAnswer(inv -> Optional.ofNullable(challenges.get(inv.getArgument(0))));

        when(ordRepo.findBySocietyIdAndClientTxUuid(anyLong(), anyString())).thenAnswer(inv -> {
            String tx = inv.getArgument(1);
            return orders.stream().filter(o -> tx.equals(o.getClientTxUuid())).findFirst();
        });
        when(ordRepo.findTopBySocietyIdOrderByCreatedAtDesc(anyLong())).thenAnswer(inv ->
                orders.isEmpty() ? Optional.empty() : Optional.of(orders.get(orders.size() - 1)));
        when(ordRepo.save(any())).thenAnswer(inv -> {
            PosOrderEntity o = inv.getArgument(0);
            orders.removeIf(existing -> existing.getOrderId().equals(o.getOrderId()));
            orders.add(o);
            return o;
        });
        when(ordRepo.findBySocietyIdAndTerminalIdAndBatchIdIsNullAndOrderedAtBetween(anyLong(), anyString(), any(), any()))
                .thenAnswer(inv -> orders.stream().filter(o -> o.getBatchId() == null).toList());

        when(batchRepo.findTopBySocietyIdAndTerminalIdAndBusinessDateOrderByBatchSeqDesc(anyLong(), anyString(), any()))
                .thenReturn(Optional.empty());
        when(batchRepo.save(any())).thenAnswer(inv -> {
            PosDailyBatchEntity b = inv.getArgument(0);
            batches.add(b);
            return b;
        });

        return new ClubhousePosService(accRepo, chRepo, ordRepo, batchRepo);
    }

    @Test
    void pinLockoutAfterFiveFailuresAndPushChallengeFlow() {
        Map<Long, FlatTabAccountEntity> accounts = new HashMap<>();
        Map<UUID, PosTabChallengeEntity> challenges = new HashMap<>();
        List<PosOrderEntity> orders = new ArrayList<>();
        List<PosDailyBatchEntity> batches = new ArrayList<>();
        ClubhousePosService service = createServiceWithInMemoryStores(accounts, challenges, orders, batches);

        // Wrong PIN 5 times triggers 15m lockout
        for (int i = 1; i <= 5; i++) {
            PosOrderRequest badReq = new PosOrderRequest(
                    1L, 1204L, "Tower A - 1204", "CAFE-POS-1", "Brew & Bean Café",
                    "tx-bad-" + i, "Cold Brew x1", 18000L, 900L, "FLAT_TAB", "PIN", "9999", null);
            assertThrows(UnauthorizedActionException.class, () -> service.authorizeOrder(badReq));
        }
        assertNotNull(accounts.get(1204L).getPinLockedUntil());

        // Clear lock for push confirmation test on Tower A - 1204
        accounts.get(1204L).setPinLockedUntil(null);
        accounts.get(1204L).setPinFailedAttempts(0);

        PosOrderRequest pushReq = new PosOrderRequest(
                1L, 1204L, "Tower A - 1204", "CAFE-POS-1", "Brew & Bean Café",
                "tx-push-1", "Cappuccino x2", 32000L, 1600L, "FLAT_TAB", "PUSH_CHALLENGE", null, null);
        PosAuthorizationResponse pending = service.authorizeOrder(pushReq);
        assertEquals("CHALLENGE_PENDING", pending.outcome());
        assertEquals(33600L, pending.account().activeHoldsPaise());

        PosAuthorizationResponse captured = service.resolvePushChallenge(
                pending.challenge().challengeId(), new ApproveChallengeRequest("resident_1204", true));
        assertEquals("CAPTURED", captured.outcome());
        assertEquals(0L, captured.account().activeHoldsPaise());
        assertEquals(33600L, captured.account().unbilledTabPaise());
        assertEquals(1, orders.size());
    }

    @Test
    void offlineSyncDedupesHonorAndFreezesOverrunThenReconcilesEodBatch() {
        Map<Long, FlatTabAccountEntity> accounts = new HashMap<>();
        Map<UUID, PosTabChallengeEntity> challenges = new HashMap<>();
        List<PosOrderEntity> orders = new ArrayList<>();
        List<PosDailyBatchEntity> batches = new ArrayList<>();
        ClubhousePosService service = createServiceWithInMemoryStores(accounts, challenges, orders, batches);

        // Pre-seed Tower A - 1204 near limit: ₹10,000 limit, ₹9,200 already unbilled (₹800 left)
        service.getOrProvisionAccount(1L, 1204L, "Tower A - 1204");
        accounts.get(1204L).setUnbilledTabPaise(920_000L);

        // Two offline orders: first ₹500 (within limit), second ₹600 (overruns limit -> Honor-and-Freeze)
        OfflineOrderSyncItem item1 = new OfflineOrderSyncItem(
                "off-uuid-1", 1204L, "Tower A - 1204", "CAFE-POS-1", "Club Café",
                "Masala Chai x2", 47619L, 2381L, "OFFLINE_QR", Instant.now());
        OfflineOrderSyncItem item2 = new OfflineOrderSyncItem(
                "off-uuid-2", 1204L, "Tower A - 1204", "CAFE-POS-1", "Club Café",
                "Club Sandwich x2", 57143L, 2857L, "OFFLINE_QR", Instant.now());

        OfflineSyncResultDto sync1 = service.syncOfflineOrders(new OfflineSyncBatchRequest(1L, List.of(item1, item2)));
        assertEquals(2, sync1.syncedCount());
        assertEquals(1, sync1.overrunFreezeCount());
        assertTrue(accounts.get(1204L).isOverdueFrozen());

        // Re-submitting item1 must deduplicate idempotently
        OfflineSyncResultDto replay = service.syncOfflineOrders(new OfflineSyncBatchRequest(1L, List.of(item1)));
        assertEquals(0, replay.syncedCount());
        assertEquals(1, replay.deduplicatedCount());

        // EOD Batch Reconciliation captures both orders and flags overrun status
        PosDailyBatchDto batch = service.closeAndReconcileDailyBatch(
                new ReconcileBatchRequest(1L, "CAFE-POS-1", LocalDate.now(ClubhousePosService.IST).toString()));
        assertEquals(2, batch.orderCount());
        assertEquals(110_000L, batch.grossPaise());
        assertEquals(1, batch.offlineOverrunCount());
        assertEquals("RECON_WITH_OFFLINE_OVERRUN", batch.erpStatus());
        assertTrue(batch.erpVoucherRef().startsWith("JV-POS-"));
    }
}
