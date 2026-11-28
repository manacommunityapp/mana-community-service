package com.manacommunity.api.handover.service;

import com.manacommunity.api.handover.entity.CorpusFundTransaction;
import com.manacommunity.api.handover.entity.CorpusFundTransaction.*;
import com.manacommunity.api.handover.entity.CutoverSnapshot;
import com.manacommunity.api.handover.entity.CutoverSnapshot.CutoverStatus;
import com.manacommunity.api.handover.repository.CorpusFundTransactionRepository;
import com.manacommunity.api.handover.repository.CutoverSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorpusCutoverService {

    private final CorpusFundTransactionRepository txnRepo;
    private final CutoverSnapshotRepository snapshotRepo;

    @Transactional
    public CutoverSnapshot createSnapshot(String builderName, int totalFlats, int possessionDone) {
        List<CorpusFundTransaction> all = txnRepo.findAll();

        double totalCorpus = all.stream()
                .filter(t -> t.getType() == TransactionType.BUILDER_DEPOSIT || t.getType() == TransactionType.OPENING_BALANCE)
                .mapToDouble(CorpusFundTransaction::getNetAmount).sum();
        double totalExpenses = all.stream()
                .filter(t -> t.getType() != TransactionType.BUILDER_DEPOSIT
                        && t.getType() != TransactionType.OPENING_BALANCE
                        && t.getType() != TransactionType.INTEREST_EARNED
                        && t.getType() != TransactionType.ESCROW_HOLD
                        && t.getType() != TransactionType.ESCROW_RELEASE)
                .mapToDouble(CorpusFundTransaction::getNetAmount).sum();
        double escrowHeld = all.stream().filter(t -> t.getType() == TransactionType.ESCROW_HOLD)
                .mapToDouble(CorpusFundTransaction::getNetAmount).sum();
        double escrowReleased = all.stream().filter(t -> t.getType() == TransactionType.ESCROW_RELEASE)
                .mapToDouble(CorpusFundTransaction::getNetAmount).sum();

        int verified = (int) all.stream().filter(t -> t.getStatus() == TransactionStatus.VERIFIED).count();
        int disputed = (int) all.stream().filter(t -> t.getStatus() == TransactionStatus.DISPUTED).count();
        int pending = (int) all.stream().filter(t -> t.getStatus() == TransactionStatus.PENDING_VERIFICATION).count();

        double openingBalance = totalCorpus - Math.abs(totalExpenses);

        CutoverSnapshot snap = CutoverSnapshot.builder()
                .communityId(1L).projectId(1L)
                .snapshotCode("CUT-" + System.currentTimeMillis() % 100000)
                .status(CutoverStatus.DRAFT)
                .cutoverDate(LocalDate.now())
                .financialYearFrom("2025-26").financialYearTo("2026-27")
                .totalCorpusFund(totalCorpus)
                .maintenanceReserve(totalCorpus * 0.5)
                .sinkingFund(totalCorpus * 0.2)
                .repairFund(totalCorpus * 0.15)
                .insuranceFund(totalCorpus * 0.15)
                .totalEscrowHeld(escrowHeld - escrowReleased)
                .totalExpenses18Months(Math.abs(totalExpenses))
                .totalVouchers(all.size())
                .verifiedVouchers(verified)
                .disputedVouchers(disputed)
                .pendingVouchers(pending)
                .openingBalanceRwa(openingBalance)
                .builderSignatoryName(builderName)
                .builderSignatoryDesignation("Project Director")
                .totalFlats(totalFlats)
                .possessionCompleted(possessionDone)
                .possessionPending(totalFlats - possessionDone)
                .handoverCompletionPercent(possessionDone * 100.0 / Math.max(totalFlats, 1))
                .build();

        return snapshotRepo.save(snap);
    }

    @Transactional
    public CutoverSnapshot freezeOpeningBalance(Long snapshotId, String frozenByName) {
        CutoverSnapshot snap = snapshotRepo.findById(snapshotId)
                .orElseThrow(() -> new RuntimeException("Snapshot not found"));
        snap.setOpeningBalanceFrozen(true);
        snap.setFrozenAt(LocalDateTime.now());
        snap.setFrozenByName(frozenByName);
        snap.setStatus(CutoverStatus.OPENING_BALANCE_FROZEN);

        txnRepo.findAll().forEach(t -> {
            t.setFrozen(true);
            t.setCutoverPhase(CutoverPhase.SNAPSHOT_FROZEN);
            txnRepo.save(t);
        });

        return snapshotRepo.save(snap);
    }

    @Transactional
    public CorpusFundTransaction disputeTransaction(Long txnId, String reason, String raisedBy) {
        CorpusFundTransaction txn = txnRepo.findById(txnId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        txn.setStatus(TransactionStatus.DISPUTED);
        txn.setDisputeReason(reason);
        txn.setDisputeRaisedBy(raisedBy);
        txn.setDisputeRaisedAt(LocalDateTime.now());
        return txnRepo.save(txn);
    }

    @Transactional
    public CorpusFundTransaction resolveDispute(Long txnId, String resolution) {
        CorpusFundTransaction txn = txnRepo.findById(txnId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        txn.setStatus(TransactionStatus.VERIFIED);
        txn.setDisputeResolution(resolution);
        txn.setDisputeResolvedAt(LocalDateTime.now());
        return txnRepo.save(txn);
    }

    @Transactional
    public CorpusFundTransaction createEscrowHold(double amount, String description, String reason) {
        CorpusFundTransaction txn = CorpusFundTransaction.builder()
                .communityId(1L).projectId(1L)
                .voucherNumber("ESC-" + System.currentTimeMillis() % 100000)
                .type(TransactionType.ESCROW_HOLD)
                .status(TransactionStatus.VERIFIED)
                .cutoverPhase(CutoverPhase.CUTOVER_IN_PROGRESS)
                .amount(amount).gstAmount(0).tdsDeducted(0).netAmount(amount)
                .description(description).category("Escrow")
                .transactionDate(LocalDate.now()).financialYear("2026-27")
                .monthNumber(LocalDate.now().getMonthValue())
                .paidToOrReceivedFrom("Escrow Account")
                .paymentMode("INTERNAL_TRANSFER")
                .disputeReason(reason)
                .partOfCutover(true)
                .build();
        return txnRepo.save(txn);
    }

    public CutoverDashboard getDashboard() {
        List<CorpusFundTransaction> all = txnRepo.findAll();
        CutoverSnapshot latestSnap = snapshotRepo.findTopByProjectIdOrderByCreatedAtDesc(1L).orElse(null);

        double totalInflows = all.stream()
                .filter(t -> t.getNetAmount() > 0)
                .mapToDouble(CorpusFundTransaction::getNetAmount).sum();
        double totalOutflows = all.stream()
                .filter(t -> t.getNetAmount() < 0)
                .mapToDouble(t -> Math.abs(t.getNetAmount())).sum();

        Map<String, Double> byType = new LinkedHashMap<>();
        txnRepo.sumByType().forEach(r -> byType.put((String) r[0], ((Number) r[1]).doubleValue()));

        Map<String, Double> expenseBreakdown = new LinkedHashMap<>();
        txnRepo.expenseByCategory().forEach(r -> {
            if (r[0] != null) expenseBreakdown.put((String) r[0], Math.abs(((Number) r[1]).doubleValue()));
        });

        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(t -> t.getStatus().name(), Collectors.counting()));

        List<CorpusFundTransaction> disputed = txnRepo.findDisputedTransactions();
        List<CorpusFundTransaction> escrowHolds = txnRepo.findActiveEscrowHolds();

        double totalGst = all.stream().mapToDouble(CorpusFundTransaction::getGstAmount).sum();
        double totalTds = all.stream().mapToDouble(CorpusFundTransaction::getTdsDeducted).sum();

        return new CutoverDashboard(all.size(), totalInflows, totalOutflows, totalInflows - totalOutflows,
                totalGst, totalTds, byType, expenseBreakdown, byStatus,
                disputed.size(), escrowHolds.size(),
                latestSnap, all, disputed, escrowHolds);
    }

    public record CutoverDashboard(int totalTransactions, double totalInflows, double totalOutflows, double netBalance,
                                     double totalGst, double totalTds,
                                     Map<String, Double> amountByType, Map<String, Double> expenseBreakdown,
                                     Map<String, Long> byStatus,
                                     int disputedCount, int escrowHoldCount,
                                     CutoverSnapshot latestSnapshot,
                                     List<CorpusFundTransaction> transactions,
                                     List<CorpusFundTransaction> disputedTransactions,
                                     List<CorpusFundTransaction> escrowHolds) {}
}
