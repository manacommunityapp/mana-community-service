package com.manacommunity.api.finance.wallet.service;

import com.manacommunity.api.finance.wallet.dto.WalletDtos.*;
import com.manacommunity.api.finance.wallet.entity.*;
import com.manacommunity.api.finance.wallet.repository.*;
import com.manacommunity.api.platform.audit.AuditJournalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityWalletService {

    private final CommunityResidentWalletRepository walletRepo;
    private final CommunityWalletTransactionRepository txnRepo;
    private final CommunityWalletCreditLineRepository creditRepo;
    private final AuditJournalService audit;

    @Transactional
    public CommunityResidentWallet getOrCreateWallet(Long residentId, Long communityId) {
        return walletRepo.findByResidentIdAndCommunityId(residentId, communityId)
                .orElseGet(() -> walletRepo.save(CommunityResidentWallet.builder()
                        .residentId(residentId)
                        .communityId(communityId)
                        .build()));
    }

    /** Stub top-up — returns a fake payment URL; real gateway integration plugs in here. */
    @Transactional
    public TopupInitResult initiateTopup(Long residentId, Long communityId,
                                         BigDecimal amount, String paymentMethod) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Top-up amount must be positive");
        }
        String ref = "STUB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        log.info("STUB topup initiated: resident={} amount={} ref={}", residentId, amount, ref);
        return new TopupInitResult("https://pay.mana.app/stub?ref=" + ref, ref, "PENDING");
    }

    /** Called by payment gateway callback — credits wallet balance. */
    @Transactional
    public CommunityWalletTransaction confirmTopup(Long residentId, Long communityId,
                                                    String externalTxnRef, BigDecimal amount) {
        CommunityResidentWallet wallet = getOrCreateWallet(residentId, communityId);
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setUpdatedAt(OffsetDateTime.now());
        walletRepo.save(wallet);
        return recordLedger(wallet, "TOPUP", "CREDIT", amount,
                null, null, "Wallet top-up", null, externalTxnRef);
    }

    /** Pay maintenance invoice; falls back to credit line if balance insufficient. */
    @Transactional
    public CommunityWalletTransaction payInvoice(Long residentId, Long communityId,
                                                   Long invoiceId, BigDecimal amount) {
        CommunityResidentWallet wallet = getOrCreateWallet(residentId, communityId);
        debit(wallet, amount, "INVOICE");
        return recordLedger(wallet, "MAINTENANCE_PAYMENT", "DEBIT", amount,
                "INVOICE", invoiceId, "Maintenance invoice #" + invoiceId, "WALLET", null);
    }

    @Transactional
    public CommunityWalletTransaction payBooking(Long residentId, Long communityId,
                                                  Long bookingId, BigDecimal amount) {
        CommunityResidentWallet wallet = getOrCreateWallet(residentId, communityId);
        debit(wallet, amount, "BOOKING");
        return recordLedger(wallet, "AMENITY_BOOKING", "DEBIT", amount,
                "BOOKING", bookingId, "Amenity booking #" + bookingId, "WALLET", null);
    }

    @Transactional
    public CommunityWalletTransaction settleGroupExpense(Long residentId, Long communityId,
                                                          Long expenseId, BigDecimal amount) {
        CommunityResidentWallet wallet = getOrCreateWallet(residentId, communityId);
        debit(wallet, amount, "GROUP_EXPENSE");
        return recordLedger(wallet, "GROUP_EXPENSE_SETTLE", "DEBIT", amount,
                "GROUP_EXPENSE", expenseId, "Group expense #" + expenseId, "WALLET", null);
    }

    /** Admin/committee approves a credit line for a resident. */
    @Transactional
    public CommunityWalletCreditLine approveCreditLine(Long residentId, Long communityId,
                                                        BigDecimal amount, Long approvedBy,
                                                        int repaymentDays) {
        CommunityResidentWallet wallet = getOrCreateWallet(residentId, communityId);
        wallet.setCreditLimit(wallet.getCreditLimit().add(amount));
        walletRepo.save(wallet);
        CommunityWalletCreditLine line = CommunityWalletCreditLine.builder()
                .walletId(wallet.getId())
                .approvedBy(approvedBy)
                .approvedAmount(amount)
                .repaymentDays(repaymentDays)
                .expiresAt(OffsetDateTime.now().plusDays(repaymentDays))
                .build();
        audit.recordFinancial("CREDIT_LINE_APPROVED", "CommunityWallet", wallet.getId().toString(),
                communityId, "amount=" + amount + " approvedBy=" + approvedBy);
        return creditRepo.save(line);
    }

    /** FM/admin issues a refund — reverses the original transaction. */
    @Transactional
    public CommunityWalletTransaction refund(Long originalTxnId, String reason) {
        CommunityWalletTransaction original = txnRepo.findById(originalTxnId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + originalTxnId));
        CommunityResidentWallet wallet = walletRepo.findById(original.getWalletId())
                .orElseThrow(() -> new IllegalStateException("Wallet not found"));
        wallet.setBalance(wallet.getBalance().add(original.getAmount()));
        wallet.setUpdatedAt(OffsetDateTime.now());
        walletRepo.save(wallet);
        original.setStatus("REVERSED");
        txnRepo.save(original);
        return recordLedger(wallet, "REFUND", "CREDIT", original.getAmount(),
                original.getReferenceType(), original.getReferenceId(),
                "Refund: " + reason, null, null);
    }

    public BigDecimal getBalance(Long residentId, Long communityId) {
        return getOrCreateWallet(residentId, communityId).getBalance();
    }

    public Page<CommunityWalletTransaction> getTransactionHistory(Long residentId, Long communityId, Pageable p) {
        CommunityResidentWallet wallet = getOrCreateWallet(residentId, communityId);
        return txnRepo.findByWalletIdOrderByInitiatedAtDesc(wallet.getId(), p);
    }

    public Page<CommunityWalletTransaction> getCommunityLedger(Long communityId, String type, Pageable p) {
        if (type != null && !type.isBlank()) {
            return txnRepo.findByCommunityIdAndTxnTypeOrderByInitiatedAtDesc(communityId, type, p);
        }
        return txnRepo.findByCommunityIdOrderByInitiatedAtDesc(communityId, p);
    }

    // ── private helpers ────────────────────────────────────────────────────

    /** Must run inside an active @Transactional. Deducts balance; uses credit if needed. */
    private void debit(CommunityResidentWallet wallet, BigDecimal amount, String context) {
        if (wallet.getBalance().compareTo(amount) >= 0) {
            wallet.setBalance(wallet.getBalance().subtract(amount));
        } else {
            BigDecimal shortfall = amount.subtract(wallet.getBalance());
            BigDecimal available = wallet.getCreditLimit().subtract(
                    creditRepo.findByWalletIdAndStatus(wallet.getId(), "ACTIVE")
                              .stream().map(l -> l.getDrawnAmount())
                              .reduce(BigDecimal.ZERO, BigDecimal::add));
            if (available.compareTo(shortfall) < 0) {
                throw new IllegalStateException(
                        "Insufficient balance and credit limit for " + context);
            }
            wallet.setBalance(BigDecimal.ZERO);
            // mark drawdown on first active credit line
            creditRepo.findByWalletIdAndStatus(wallet.getId(), "ACTIVE").stream().findFirst()
                    .ifPresent(line -> {
                        line.setDrawnAmount(line.getDrawnAmount().add(shortfall));
                        creditRepo.save(line);
                    });
        }
        wallet.setUpdatedAt(OffsetDateTime.now());
        walletRepo.save(wallet);
    }

    /** Append ledger entry — Propagation.MANDATORY enforced by calling methods being @Transactional. */
    @Transactional(propagation = Propagation.MANDATORY)
    public CommunityWalletTransaction recordLedger(CommunityResidentWallet wallet,
                                                    String txnType, String direction,
                                                    BigDecimal amount,
                                                    String refType, Long refId,
                                                    String description,
                                                    String paymentMethod,
                                                    String externalRef) {
        CommunityWalletTransaction txn = CommunityWalletTransaction.builder()
                .walletId(wallet.getId())
                .communityId(wallet.getCommunityId())
                .txnType(txnType)
                .direction(direction)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .referenceType(refType)
                .referenceId(refId)
                .description(description)
                .paymentMethod(paymentMethod)
                .externalTxnRef(externalRef)
                .status("COMPLETED")
                .completedAt(OffsetDateTime.now())
                .build();
        audit.recordFinancial(txnType, "CommunityWallet", wallet.getId().toString(),
                wallet.getCommunityId(), direction + " " + amount);
        return txnRepo.save(txn);
    }
}
