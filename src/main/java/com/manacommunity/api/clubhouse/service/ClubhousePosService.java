package com.manacommunity.api.clubhouse.service;

import com.manacommunity.api.clubhouse.dto.ClubhouseDtos.*;
import com.manacommunity.api.clubhouse.entity.*;
import com.manacommunity.api.clubhouse.repository.*;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Offline-resilient Clubhouse Café POS, flat-tab credit verification, 2FA PIN/Push challenge,
 * and EOD ERP batch reconciliation service.
 */
@Service
@RequiredArgsConstructor
public class ClubhousePosService {

    static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    static final int MAX_PIN_ATTEMPTS = 5;
    static final int PIN_LOCK_MINUTES = 15;
    static final int CHALLENGE_TTL_SECONDS = 90;
    static final String DEFAULT_DEMO_PIN = "4829";
    static final String GENESIS_HASH = "0".repeat(64);

    private final FlatTabAccountRepository accountRepository;
    private final PosTabChallengeRepository challengeRepository;
    private final PosOrderRepository orderRepository;
    private final PosDailyBatchRepository batchRepository;

    @Transactional
    public FlatTabAccountDto getOrProvisionAccount(long societyId, long flatId, String unitCode) {
        FlatTabAccountEntity account = findOrCreateAccount(societyId, flatId, unitCode);
        return toAccountDto(account);
    }

    @Transactional(noRollbackFor = UnauthorizedActionException.class)
    public PosAuthorizationResponse authorizeOrder(PosOrderRequest req) {
        if (req.subtotalPaise() <= 0 || req.gstPaise() < 0) {
            throw new InvalidInputException("Order subtotal must be positive and GST non-negative");
        }
        String clientTxUuid = req.clientTxUuid() != null && !req.clientTxUuid().isBlank()
                ? req.clientTxUuid() : UUID.randomUUID().toString();

        // Idempotent replay guard
        Optional<PosOrderEntity> existing = orderRepository.findBySocietyIdAndClientTxUuid(req.societyId(), clientTxUuid);
        FlatTabAccountEntity account = findOrCreateAccount(req.societyId(), req.flatId(), req.unitCode());
        if (existing.isPresent()) {
            return new PosAuthorizationResponse("IDEMPOTENT_REPLAY", null, toOrderDto(existing.get()), toAccountDto(account));
        }

        long totalPaise = req.subtotalPaise() + req.gstPaise();
        String settlementMode = normalizeSettlementMode(req.settlementMode());
        if (!"PREPAID_WALLET".equals(settlementMode)) {
            if (account.isOverdueFrozen()) {
                throw new InvalidInputException("Flat tab is frozen due to overdue dues or offline overrun; settle via UPI/Wallet");
            }
            if (account.availableCreditPaise() < totalPaise) {
                throw new InvalidInputException("Insufficient flat tab credit headroom");
            }
        }

        String authMethod = req.authMethod() != null ? req.authMethod().trim().toUpperCase(Locale.ROOT) : "PUSH_CHALLENGE";
        if ("PIN".equals(authMethod)) {
            verifyResidentPinOrThrow(account, req.pin());
            PosOrderDto captured = captureOrderInternal(account, req, clientTxUuid, null, settlementMode, "PIN", false, false);
            return new PosAuthorizationResponse("CAPTURED", null, captured, toAccountDto(account));
        }
        if ("OFFLINE_QR".equals(authMethod)) {
            verifyOfflineTotpQrOrThrow(account, req.offlineTotpQr());
            PosOrderDto captured = captureOrderInternal(account, req, clientTxUuid, null, settlementMode, "OFFLINE_QR", false, false);
            return new PosAuthorizationResponse("CAPTURED", null, captured, toAccountDto(account));
        }

        // Push Confirmation: place atomic headroom hold for 90s while resident taps Approve
        if (!"PREPAID_WALLET".equals(settlementMode)) {
            account.setActiveHoldsPaise(account.getActiveHoldsPaise() + totalPaise);
            account.setUpdatedAt(Instant.now());
            accountRepository.save(account);
        }

        PosTabChallengeEntity challenge = new PosTabChallengeEntity();
        challenge.setChallengeId(UUID.randomUUID());
        challenge.setSocietyId(req.societyId());
        challenge.setFlatId(account.getFlatId());
        challenge.setUnitCode(account.getUnitCode());
        challenge.setTerminalId(req.terminalId());
        challenge.setOutletName(req.outletName());
        challenge.setClientTxUuid(clientTxUuid);
        challenge.setSubtotalPaise(req.subtotalPaise());
        challenge.setGstPaise(req.gstPaise());
        challenge.setTotalPaise(totalPaise);
        challenge.setItemsSummary(req.itemsSummary());
        challenge.setAuthMethod("PUSH_CHALLENGE");
        challenge.setStatus("PENDING");
        challenge.setExpiresAt(Instant.now().plus(CHALLENGE_TTL_SECONDS, ChronoUnit.SECONDS));
        challengeRepository.save(challenge);

        return new PosAuthorizationResponse("CHALLENGE_PENDING", toChallengeDto(challenge), null, toAccountDto(account));
    }

    @Transactional
    public PosAuthorizationResponse resolvePushChallenge(UUID challengeId, ApproveChallengeRequest req) {
        PosTabChallengeEntity challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("POS tab challenge not found: " + challengeId));
        if (!"PENDING".equals(challenge.getStatus())) {
            throw new InvalidInputException("Challenge already resolved: " + challenge.getStatus());
        }

        FlatTabAccountEntity account = findOrCreateAccount(
                challenge.getSocietyId(), challenge.getFlatId(), challenge.getUnitCode());
        long holdAmount = challenge.getTotalPaise();
        account.setActiveHoldsPaise(Math.max(0L, account.getActiveHoldsPaise() - holdAmount));
        account.setUpdatedAt(Instant.now());

        if (Instant.now().isAfter(challenge.getExpiresAt())) {
            challenge.setStatus("EXPIRED");
            challengeRepository.save(challenge);
            accountRepository.save(account);
            throw new InvalidInputException("Push confirmation expired after " + CHALLENGE_TTL_SECONDS + "s");
        }

        if (!req.approve()) {
            challenge.setStatus("DECLINED");
            challenge.setApprovedBy(req.approvedBy());
            challengeRepository.save(challenge);
            accountRepository.save(account);
            return new PosAuthorizationResponse("DECLINED", toChallengeDto(challenge), null, toAccountDto(account));
        }

        challenge.setStatus("APPROVED");
        challenge.setApprovedBy(req.approvedBy() != null ? req.approvedBy() : "RESIDENT_APP");
        challengeRepository.save(challenge);

        PosOrderRequest orderReq = new PosOrderRequest(
                challenge.getSocietyId(),
                challenge.getFlatId(),
                challenge.getUnitCode(),
                challenge.getTerminalId(),
                challenge.getOutletName(),
                challenge.getClientTxUuid(),
                challenge.getItemsSummary(),
                challenge.getSubtotalPaise(),
                challenge.getGstPaise(),
                account.getBillingPreference(),
                "PUSH_CHALLENGE",
                null,
                null
        );
        PosOrderDto captured = captureOrderInternal(
                account, orderReq, challenge.getClientTxUuid(), challenge.getChallengeId(),
                account.getBillingPreference(), "PUSH_CHALLENGE", false, false);
        return new PosAuthorizationResponse("CAPTURED", toChallengeDto(challenge), captured, toAccountDto(account));
    }

    @Transactional
    public OfflineSyncResultDto syncOfflineOrders(OfflineSyncBatchRequest batchReq) {
        List<PosOrderDto> syncedOrders = new ArrayList<>();
        int synced = 0;
        int deduped = 0;
        int overrunFreeze = 0;

        for (OfflineOrderSyncItem item : batchReq.items()) {
            Optional<PosOrderEntity> existing = orderRepository.findBySocietyIdAndClientTxUuid(
                    batchReq.societyId(), item.clientTxUuid());
            if (existing.isPresent()) {
                syncedOrders.add(toOrderDto(existing.get()));
                deduped++;
                continue;
            }

            FlatTabAccountEntity account = findOrCreateAccount(
                    batchReq.societyId(), item.flatId(), item.unitCode());
            long total = item.subtotalPaise() + item.gstPaise();
            long availableBefore = account.availableCreditPaise();
            boolean overrun = account.isOverdueFrozen() || availableBefore < total || total > account.getOfflineFloorCapPaise();

            // Honor-and-Freeze: honor signed offline café receipt for settlement while freezing further tab charges on overrun
            if (overrun) {
                account.setOverdueFrozen(true);
                overrunFreeze++;
            }

            PosOrderRequest syntheticReq = new PosOrderRequest(
                    batchReq.societyId(),
                    account.getFlatId(),
                    account.getUnitCode(),
                    item.terminalId(),
                    item.outletName(),
                    item.clientTxUuid(),
                    item.itemsSummary(),
                    item.subtotalPaise(),
                    item.gstPaise(),
                    "FLAT_TAB",
                    item.authMethod() != null ? item.authMethod() : "OFFLINE_QR",
                    null,
                    null
            );
            PosOrderDto dto = captureOrderInternal(
                    account, syntheticReq, item.clientTxUuid(), null,
                    "FLAT_TAB", syntheticReq.authMethod(), true, overrun, item.orderedAt());
            syncedOrders.add(dto);
            synced++;
        }

        return new OfflineSyncResultDto(batchReq.items().size(), synced, deduped, overrunFreeze, syncedOrders);
    }

    @Transactional(readOnly = true)
    public OfflineTerminalSnapshotDto getOfflineTerminalSnapshot(long societyId) {
        List<FlatTabAccountEntity> accounts = accountRepository.findBySocietyIdOrderByUnitCodeAsc(societyId);
        List<OfflineSnapshotEntry> entries = accounts.stream()
                .map(a -> new OfflineSnapshotEntry(
                        a.getFlatId(),
                        a.getUnitCode(),
                        Math.max(0L, a.availableCreditPaise()),
                        a.getOfflineFloorCapPaise(),
                        a.isOverdueFrozen(),
                        a.getPinHash().substring(0, 16)
                ))
                .toList();
        String now = Instant.now().toString();
        String sig = ClubhouseCryptoUtil.hmacSha256Hex(societyId + ":" + now + ":" + entries.size());
        return new OfflineTerminalSnapshotDto(societyId, now, sig, entries);
    }

    @Transactional(readOnly = true)
    public List<PosOrderDto> listOrders(long societyId) {
        return orderRepository.findBySocietyIdOrderByOrderedAtDesc(societyId)
                .stream().map(this::toOrderDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PosTabChallengeDto> listPendingChallenges(long societyId, long flatId) {
        return challengeRepository.findBySocietyIdAndFlatIdAndStatusOrderByCreatedAtDesc(societyId, flatId, "PENDING")
                .stream().map(this::toChallengeDto).toList();
    }

    @Transactional
    public PosDailyBatchDto closeAndReconcileDailyBatch(ReconcileBatchRequest req) {
        LocalDate date = req.businessDate() != null && !req.businessDate().isBlank()
                ? LocalDate.parse(req.businessDate())
                : LocalDate.now(IST);
        Instant start = date.atStartOfDay(IST).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(IST).toInstant().minusNanos(1);

        List<PosOrderEntity> unbatched = orderRepository
                .findBySocietyIdAndTerminalIdAndBatchIdIsNullAndOrderedAtBetween(
                        req.societyId(), req.terminalId(), start, end);

        int nextSeq = batchRepository
                .findTopBySocietyIdAndTerminalIdAndBusinessDateOrderByBatchSeqDesc(req.societyId(), req.terminalId(), date)
                .map(b -> b.getBatchSeq() + 1)
                .orElse(1);

        long gross = 0L;
        long gst = 0L;
        long flatTab = 0L;
        long wallet = 0L;
        int overruns = 0;
        String hashRoot = GENESIS_HASH;

        UUID batchId = UUID.randomUUID();
        for (PosOrderEntity order : unbatched) {
            gross += order.getTotalPaise();
            gst += order.getGstPaise();
            if ("PREPAID_WALLET".equals(order.getSettlementMode())) {
                wallet += order.getTotalPaise();
            } else {
                flatTab += order.getTotalPaise();
            }
            if (order.isOfflineOverrun()) {
                overruns++;
            }
            hashRoot = order.getHashChainDigest();
            order.setBatchId(batchId);
            order.setStatus("BATCH_SETTLED");
            orderRepository.save(order);
        }

        String erpStatus = overruns > 0 ? "RECON_WITH_OFFLINE_OVERRUN" : "MATCHED";
        String voucherRef = "JV-POS-" + date + "-" + req.terminalId() + "-B" + nextSeq;

        PosDailyBatchEntity batch = new PosDailyBatchEntity();
        batch.setBatchId(batchId);
        batch.setSocietyId(req.societyId());
        batch.setTerminalId(req.terminalId());
        batch.setBusinessDate(date);
        batch.setBatchSeq(nextSeq);
        batch.setOrderCount(unbatched.size());
        batch.setGrossPaise(gross);
        batch.setGstPaise(gst);
        batch.setFlatTabPaise(flatTab);
        batch.setWalletPaise(wallet);
        batch.setOfflineOverrunCount(overruns);
        batch.setHashChainRoot(hashRoot);
        batch.setErpStatus(erpStatus);
        batch.setErpVoucherRef(voucherRef);
        batch.setClosedAt(Instant.now());
        batchRepository.save(batch);

        return toBatchDto(batch);
    }

    @Transactional(readOnly = true)
    public List<PosDailyBatchDto> listBatches(long societyId) {
        return batchRepository.findBySocietyIdOrderByClosedAtDesc(societyId)
                .stream().map(this::toBatchDto).toList();
    }

    private PosOrderDto captureOrderInternal(
            FlatTabAccountEntity account,
            PosOrderRequest req,
            String clientTxUuid,
            UUID challengeId,
            String settlementMode,
            String authMethod,
            boolean provisionalOffline,
            boolean offlineOverrun) {
        return captureOrderInternal(
                account, req, clientTxUuid, challengeId, settlementMode,
                authMethod, provisionalOffline, offlineOverrun, Instant.now());
    }

    private PosOrderDto captureOrderInternal(
            FlatTabAccountEntity account,
            PosOrderRequest req,
            String clientTxUuid,
            UUID challengeId,
            String settlementMode,
            String authMethod,
            boolean provisionalOffline,
            boolean offlineOverrun,
            Instant orderedAt) {
        long totalPaise = req.subtotalPaise() + req.gstPaise();
        if (!"PREPAID_WALLET".equals(settlementMode)) {
            account.setUnbilledTabPaise(account.getUnbilledTabPaise() + totalPaise);
        }
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);

        String prevHash = orderRepository.findTopBySocietyIdOrderByCreatedAtDesc(req.societyId())
                .map(PosOrderEntity::getHashChainDigest)
                .orElse(GENESIS_HASH);
        String digest = ClubhouseCryptoUtil.sha256Hex(prevHash + "|" + clientTxUuid + "|" + totalPaise + "|" + account.getFlatId());

        PosOrderEntity order = new PosOrderEntity();
        order.setOrderId(UUID.randomUUID());
        order.setSocietyId(req.societyId());
        order.setFlatId(account.getFlatId());
        order.setUnitCode(account.getUnitCode());
        order.setTerminalId(req.terminalId());
        order.setOutletName(req.outletName());
        order.setClientTxUuid(clientTxUuid);
        order.setChallengeId(challengeId);
        order.setItemsSummary(req.itemsSummary());
        order.setSubtotalPaise(req.subtotalPaise());
        order.setGstPaise(req.gstPaise());
        order.setTotalPaise(totalPaise);
        order.setSettlementMode(settlementMode);
        order.setAuthMethod(authMethod);
        order.setStatus("CAPTURED");
        order.setProvisionalOffline(provisionalOffline);
        order.setOfflineOverrun(offlineOverrun);
        order.setPrevOrderHash(prevHash);
        order.setHashChainDigest(digest);
        order.setOrderedAt(orderedAt != null ? orderedAt : Instant.now());
        orderRepository.save(order);
        return toOrderDto(order);
    }

    private void verifyResidentPinOrThrow(FlatTabAccountEntity account, String pin) {
        Instant now = Instant.now();
        if (account.getPinLockedUntil() != null && account.getPinLockedUntil().isAfter(now)) {
            throw new UnauthorizedActionException("Flat tab PIN is locked until " + account.getPinLockedUntil());
        }
        if (pin == null || !pin.matches("\\d{4}")) {
            throw new InvalidInputException("Resident PIN must be exactly 4 digits");
        }
        String candidateHash = ClubhouseCryptoUtil.hashPin(account.getSocietyId(), account.getFlatId(), pin);
        if (!ClubhouseCryptoUtil.constantTimeEquals(account.getPinHash(), candidateHash)) {
            int nextFails = account.getPinFailedAttempts() + 1;
            account.setPinFailedAttempts(nextFails);
            if (nextFails >= MAX_PIN_ATTEMPTS) {
                account.setPinLockedUntil(now.plus(PIN_LOCK_MINUTES, ChronoUnit.MINUTES));
            }
            account.setUpdatedAt(now);
            accountRepository.save(account);
            throw new UnauthorizedActionException("Invalid resident PIN (attempt " + nextFails + " of " + MAX_PIN_ATTEMPTS + ")");
        }
        account.setPinFailedAttempts(0);
        account.setPinLockedUntil(null);
        accountRepository.save(account);
    }

    private void verifyOfflineTotpQrOrThrow(FlatTabAccountEntity account, String offlineQr) {
        if (offlineQr == null || !offlineQr.startsWith("MANA-POS-QR:")) {
            throw new UnauthorizedActionException("Invalid offline dynamic PIN QR payload");
        }
        String[] parts = offlineQr.split(":");
        if (parts.length < 4) {
            throw new UnauthorizedActionException("Malformed offline dynamic PIN QR");
        }
        long qrFlatId = Long.parseLong(parts[1]);
        if (qrFlatId != account.getFlatId()) {
            throw new UnauthorizedActionException("Offline QR flat mismatch");
        }
        String expectedSig = ClubhouseCryptoUtil.hmacSha256Hex(parts[1] + ":" + parts[2]).substring(0, 12);
        if (!ClubhouseCryptoUtil.constantTimeEquals(expectedSig, parts[3])) {
            throw new UnauthorizedActionException("Offline dynamic PIN QR signature check failed");
        }
    }

    private FlatTabAccountEntity findOrCreateAccount(long societyId, long flatId, String unitCode) {
        return accountRepository.findBySocietyIdAndFlatId(societyId, flatId)
                .orElseGet(() -> {
                    FlatTabAccountEntity acc = new FlatTabAccountEntity();
                    acc.setAccountId(UUID.randomUUID());
                    acc.setSocietyId(societyId);
                    acc.setFlatId(flatId);
                    acc.setUnitCode(unitCode != null && !unitCode.isBlank() ? unitCode : "Tower A - " + flatId);
                    acc.setCreditLimitPaise(1_000_000L);
                    acc.setOfflineFloorCapPaise(150_000L);
                    acc.setPinHash(ClubhouseCryptoUtil.hashPin(societyId, flatId, DEFAULT_DEMO_PIN));
                    return accountRepository.save(acc);
                });
    }

    private static String normalizeSettlementMode(String mode) {
        if (mode == null || mode.isBlank()) return "FLAT_TAB";
        return mode.trim().toUpperCase(Locale.ROOT);
    }

    private FlatTabAccountDto toAccountDto(FlatTabAccountEntity a) {
        return new FlatTabAccountDto(
                a.getAccountId(), a.getSocietyId(), a.getFlatId(), a.getUnitCode(),
                a.getCreditLimitPaise(), a.getUnbilledTabPaise(), a.getActiveHoldsPaise(),
                a.availableCreditPaise(), a.getOfflineFloorCapPaise(), a.getBillingPreference(),
                a.isOverdueFrozen(), a.getPinFailedAttempts(), a.getPinLockedUntil()
        );
    }

    private PosTabChallengeDto toChallengeDto(PosTabChallengeEntity c) {
        return new PosTabChallengeDto(
                c.getChallengeId(), c.getSocietyId(), c.getFlatId(), c.getUnitCode(),
                c.getTerminalId(), c.getOutletName(), c.getSubtotalPaise(), c.getGstPaise(),
                c.getTotalPaise(), c.getItemsSummary(), c.getAuthMethod(), c.getStatus(),
                c.getApprovedBy(), c.getExpiresAt(), c.getCreatedAt()
        );
    }

    private PosOrderDto toOrderDto(PosOrderEntity o) {
        return new PosOrderDto(
                o.getOrderId(), o.getSocietyId(), o.getFlatId(), o.getUnitCode(),
                o.getTerminalId(), o.getOutletName(), o.getClientTxUuid(), o.getItemsSummary(),
                o.getSubtotalPaise(), o.getGstPaise(), o.getTotalPaise(), o.getSettlementMode(),
                o.getAuthMethod(), o.getStatus(), o.isProvisionalOffline(), o.isOfflineOverrun(),
                o.getHashChainDigest(), o.getBatchId(), o.getOrderedAt()
        );
    }

    private PosDailyBatchDto toBatchDto(PosDailyBatchEntity b) {
        return new PosDailyBatchDto(
                b.getBatchId(), b.getSocietyId(), b.getTerminalId(), b.getBusinessDate().toString(),
                b.getBatchSeq(), b.getOrderCount(), b.getGrossPaise(), b.getGstPaise(),
                b.getFlatTabPaise(), b.getWalletPaise(), b.getOfflineOverrunCount(),
                b.getHashChainRoot(), b.getErpStatus(), b.getErpVoucherRef(), b.getClosedAt()
        );
    }
}
