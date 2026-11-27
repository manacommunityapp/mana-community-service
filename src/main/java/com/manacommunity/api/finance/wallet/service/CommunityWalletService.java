package com.manacommunity.api.finance.wallet.service;

import com.manacommunity.api.finance.wallet.dto.WalletDtos.*;
import com.manacommunity.api.finance.wallet.entity.*;
import com.manacommunity.api.finance.wallet.repository.*;
import com.manacommunity.api.payments.config.RazorpayProperties;
import com.manacommunity.api.payments.exception.PaymentVerificationException;
import com.manacommunity.api.platform.audit.AuditJournalService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityWalletService {

    private static final String TXN_TYPE_TOPUP = "TOPUP";
    private static final String STATUS_PENDING   = "PENDING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_REVERSED  = "REVERSED";

    private final CommunityResidentWalletRepository walletRepo;
    private final CommunityWalletTransactionRepository txnRepo;
    private final CommunityWalletCreditLineRepository creditRepo;
    private final AuditJournalService audit;
    private final RazorpayProperties razorpayProperties;

    /** Used only to take row locks when consuming a top-up or reversing a transaction. */
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public CommunityResidentWallet getOrCreateWallet(Long residentId, Long communityId) {
        return walletRepo.findByResidentIdAndCommunityId(residentId, communityId)
                .orElseGet(() -> walletRepo.save(CommunityResidentWallet.builder()
                        .residentId(residentId)
                        .communityId(communityId)
                        .build()));
    }

    // ── gateway signature verification ─────────────────────────────────────

    /**
     * True when a usable Razorpay secret is configured.
     *
     * <p>The placeholder values shipped in {@code application.yaml} do not count:
     * with them, every verification attempt is rejected (fail closed).
     */
    public boolean isGatewayVerificationConfigured() {
        String secret = razorpayProperties.getKeySecret();
        return razorpayProperties.isEnabled()
                && secret != null
                && !secret.isBlank()
                && !secret.toLowerCase().startsWith("placeholder");
    }

    /**
     * Verifies a Razorpay checkout signature — {@code HMAC-SHA256(orderId + "|" + paymentId)}
     * keyed with {@code app.razorpay.key-secret}, compared in constant time.
     *
     * <p>This is the single signature check shared by wallet top-ups and maintenance
     * invoice settlement. It lives here (rather than in {@code RazorpayPaymentService})
     * because that service's verifier is private and its bean only exists when
     * {@code app.razorpay.enabled=true}; this method must be callable — and must
     * reject — even when the gateway is switched off.
     *
     * @throws PaymentVerificationException (HTTP 400) when the signature is missing,
     *         malformed, mismatched, or when no gateway secret is configured.
     */
    public void requireVerifiedGatewaySignature(String orderId, String paymentId, String signature) {
        if (!isGatewayVerificationConfigured()) {
            log.error("SECURITY: payment verification attempted while no Razorpay secret is configured "
                    + "(app.razorpay.enabled={}) — rejecting.", razorpayProperties.isEnabled());
            throw new PaymentVerificationException(
                    "Payment gateway verification is not configured; payments cannot be confirmed.");
        }
        if (isBlank(orderId) || isBlank(paymentId) || isBlank(signature)) {
            throw new PaymentVerificationException(
                    "orderId, paymentId and signature are all required to confirm a payment.");
        }

        byte[] provided;
        try {
            provided = HexFormat.of().parseHex(signature.trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new PaymentVerificationException("Payment signature is malformed.");
        }

        byte[] expected = hmacSha256(orderId + "|" + paymentId, razorpayProperties.getKeySecret());
        if (!MessageDigest.isEqual(expected, provided)) {
            log.warn("SECURITY: gateway signature mismatch for orderId={} paymentId={}", orderId, paymentId);
            throw new PaymentVerificationException(
                    "Payment verification failed. If money was deducted it will be refunded automatically.");
        }
    }

    private byte[] hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("HMAC-SHA256 computation failed", e);
            throw new PaymentVerificationException("Payment verification could not be completed.");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // ── top-up ─────────────────────────────────────────────────────────────

    /**
     * Starts a top-up: persists a PENDING ledger row holding the requested amount and
     * the gateway reference, and returns the payment URL.
     *
     * <p>The amount lives in that row from now on — {@link #confirmTopup} reads it from
     * there and never from the client. Payment-URL creation is still a stub; when a real
     * gateway order is created here, the gateway's order id must be stored as the
     * {@code externalTxnRef}, because confirmation requires the signed order id to match it.
     */
    @Transactional
    public TopupInitResult initiateTopup(Long residentId, Long communityId,
                                         BigDecimal amount, String paymentMethod) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Top-up amount must be positive");
        }
        CommunityResidentWallet wallet = getOrCreateWallet(residentId, communityId);

        String ref = "STUB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        CommunityWalletTransaction pending = CommunityWalletTransaction.builder()
                .walletId(wallet.getId())
                .communityId(wallet.getCommunityId())
                .txnType(TXN_TYPE_TOPUP)
                .direction("CREDIT")
                .amount(amount)
                .balanceAfter(wallet.getBalance())   // unchanged until the top-up is confirmed
                .description("Wallet top-up (awaiting gateway confirmation)")
                .paymentMethod(paymentMethod)
                .externalTxnRef(ref)
                .status(STATUS_PENDING)
                .initiatedAt(OffsetDateTime.now())
                .build();
        txnRepo.save(pending);

        log.info("Topup initiated: resident={} community={} amount={} ref={}",
                residentId, communityId, amount, ref);
        return new TopupInitResult("https://pay.mana.app/stub?ref=" + ref, ref, STATUS_PENDING);
    }

    /**
     * Confirms a top-up after the gateway callback and credits the wallet.
     *
     * <p>Guarantees:
     * <ul>
     *   <li>the credited amount comes from the PENDING row created by
     *       {@link #initiateTopup}, never from the caller;</li>
     *   <li>the gateway signature must verify, and the signed order id must be the
     *       reference of that pending row, so a signature cannot be replayed onto
     *       another top-up;</li>
     *   <li>the pending row is locked FOR UPDATE and may be consumed exactly once —
     *       a unique index (V503) also forbids a second COMPLETED top-up for the
     *       same reference at the database level;</li>
     *   <li>the pending row must belong to the caller's own wallet and community.</li>
     * </ul>
     *
     * @return the now-COMPLETED top-up ledger row
     */
    @Transactional
    public CommunityWalletTransaction confirmTopup(Long residentId, Long communityId,
                                                   String externalTxnRef,
                                                   String gatewayOrderId,
                                                   String gatewayPaymentId,
                                                   String gatewaySignature) {
        if (isBlank(externalTxnRef)) {
            throw new IllegalArgumentException("externalTxnRef is required");
        }
        String ref = externalTxnRef.trim();

        CommunityWalletTransaction pending = txnRepo.findByExternalTxnRef(ref)
                .filter(t -> TXN_TYPE_TOPUP.equals(t.getTxnType()))
                .orElseThrow(() -> new IllegalArgumentException("Unknown top-up reference: " + ref));

        CommunityResidentWallet wallet = walletRepo.findById(pending.getWalletId())
                .orElseThrow(() -> new IllegalStateException("Wallet not found for top-up " + ref));
        if (!wallet.getResidentId().equals(residentId) || !wallet.getCommunityId().equals(communityId)) {
            log.warn("SECURITY: resident {} (community {}) tried to confirm top-up {} of wallet {}",
                    residentId, communityId, ref, pending.getWalletId());
            throw new AccessDeniedException("This top-up belongs to another resident.");
        }

        // Lock the pending row so two concurrent callbacks cannot both credit it.
        entityManager.lock(pending, LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(pending);

        if (!STATUS_PENDING.equals(pending.getStatus())) {
            log.info("Top-up {} already processed (status={}) — returning existing row", ref, pending.getStatus());
            return pending;
        }

        // The signed order must be this top-up's own gateway reference.
        if (!ref.equals(gatewayOrderId == null ? null : gatewayOrderId.trim())) {
            log.warn("SECURITY: top-up {} confirmation carried a signature for order {}", ref, gatewayOrderId);
            throw new PaymentVerificationException(
                    "The signed payment does not belong to this top-up reference.");
        }
        requireVerifiedGatewaySignature(gatewayOrderId, gatewayPaymentId, gatewaySignature);

        BigDecimal amount = pending.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Top-up " + ref + " has no positive amount to credit.");
        }

        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setUpdatedAt(OffsetDateTime.now());
        walletRepo.save(wallet);

        pending.setStatus(STATUS_COMPLETED);
        pending.setBalanceAfter(wallet.getBalance());
        pending.setDescription("Wallet top-up");
        pending.setCompletedAt(OffsetDateTime.now());
        CommunityWalletTransaction saved = txnRepo.save(pending);

        audit.recordFinancial("TOPUP", "CommunityWallet", wallet.getId().toString(),
                wallet.getCommunityId(), "CREDIT " + amount + " ref=" + ref);
        log.info("Top-up {} confirmed: wallet={} amount={} newBalance={}",
                ref, wallet.getId(), amount, wallet.getBalance());
        return saved;
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

    /**
     * FM/admin issues a refund — reverses the original transaction exactly once.
     *
     * <p>Rejects a transaction that is already REVERSED, one that is not a completed
     * debit, and one that belongs to another community. The original is marked
     * REVERSED in the same transaction as the credit, so a repeated call cannot
     * grow the balance.
     *
     * @param callerCommunityId the community of the acting admin — must match the
     *                          transaction's community
     */
    @Transactional
    public CommunityWalletTransaction refund(Long originalTxnId, Long callerCommunityId, String reason) {
        if (callerCommunityId == null) {
            throw new AccessDeniedException("No community is associated with this account.");
        }
        CommunityWalletTransaction original = txnRepo.findById(originalTxnId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + originalTxnId));

        if (!callerCommunityId.equals(original.getCommunityId())) {
            log.warn("SECURITY: community {} attempted to refund transaction {} of community {}",
                    callerCommunityId, originalTxnId, original.getCommunityId());
            throw new AccessDeniedException("This transaction belongs to another community.");
        }

        // Lock the row so two concurrent refunds cannot both see it as refundable.
        entityManager.lock(original, LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(original);

        if (STATUS_REVERSED.equals(original.getStatus())) {
            throw new IllegalStateException("Transaction " + originalTxnId + " has already been refunded.");
        }
        if (!STATUS_COMPLETED.equals(original.getStatus())) {
            throw new IllegalStateException(
                    "Only completed transactions can be refunded (status=" + original.getStatus() + ").");
        }
        if (!"DEBIT".equals(original.getDirection())) {
            throw new IllegalStateException("Only a debit can be refunded, not a " + original.getDirection() + ".");
        }

        CommunityResidentWallet wallet = walletRepo.findById(original.getWalletId())
                .orElseThrow(() -> new IllegalStateException("Wallet not found"));

        original.setStatus(STATUS_REVERSED);
        txnRepo.save(original);

        wallet.setBalance(wallet.getBalance().add(original.getAmount()));
        wallet.setUpdatedAt(OffsetDateTime.now());
        walletRepo.save(wallet);

        log.info("Refund issued for txn {} (wallet {} amount {}): {}",
                originalTxnId, wallet.getId(), original.getAmount(), reason);
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

    /** Looks up a top-up ledger row by its gateway reference (used to report duplicates). */
    public Optional<CommunityWalletTransaction> findTopupByReference(String externalTxnRef) {
        if (isBlank(externalTxnRef)) {
            return Optional.empty();
        }
        return txnRepo.findByExternalTxnRef(externalTxnRef.trim())
                .filter(t -> TXN_TYPE_TOPUP.equals(t.getTxnType()));
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
                .status(STATUS_COMPLETED)
                .completedAt(OffsetDateTime.now())
                .build();
        audit.recordFinancial(txnType, "CommunityWallet", wallet.getId().toString(),
                wallet.getCommunityId(), direction + " " + amount);
        return txnRepo.save(txn);
    }
}
