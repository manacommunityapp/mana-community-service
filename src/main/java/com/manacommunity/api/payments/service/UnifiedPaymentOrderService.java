package com.manacommunity.api.payments.service;

import com.manacommunity.api.payments.config.dto.RecordManualPaymentRequest;
import com.manacommunity.api.payments.config.service.PaymentConfigService;
import com.manacommunity.api.payments.dto.CreatePaymentOrderRequest;
import com.manacommunity.api.payments.entity.PaymentOrder;
import com.manacommunity.api.payments.gateway.ManaPayGatewayService;
import com.manacommunity.api.payments.model.PaymentGateway;
import com.manacommunity.api.payments.model.PaymentOrderStatus;
import com.manacommunity.api.payments.repository.PaymentOrderRepository;
import com.manacommunity.api.transactioncore.event.GatewayPaymentCapturedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Single entry point for all payment order lifecycle operations.
 *
 * <p>All business modules (maintenance, events, sports, food, marketplace,
 * health, amenities, trips) MUST call this service instead of talking directly
 * to Cashfree/Razorpay services. Gateway-specific services remain responsible
 * for constructing the PG request; this service owns the internal record.
 *
 * <p>Financial rules:
 * <ul>
 *   <li>All amounts are BigDecimal — never float or double.</li>
 *   <li>Status changes are validated by {@link PaymentLifecycleStateMachine}.</li>
 *   <li>Idempotency key is UNIQUE — duplicate {@code createOrder} calls return
 *       the existing record without creating a second order.</li>
 *   <li>No record is ever physically deleted.</li>
 * </ul>
 */
@Slf4j
@Service
public class UnifiedPaymentOrderService {

    private final PaymentOrderRepository      repository;
    private final PaymentLifecycleStateMachine stateMachine;
    private final ApplicationEventPublisher   eventPublisher;
    private final PaymentConfigService        paymentConfigService;
    private final ManaPayGatewayService       manaPayGatewayService;

    public UnifiedPaymentOrderService(
            PaymentOrderRepository repository,
            PaymentLifecycleStateMachine stateMachine,
            ApplicationEventPublisher eventPublisher,
            @Lazy PaymentConfigService paymentConfigService,
            ManaPayGatewayService manaPayGatewayService) {
        this.repository             = repository;
        this.stateMachine           = stateMachine;
        this.eventPublisher         = eventPublisher;
        this.paymentConfigService   = paymentConfigService;
        this.manaPayGatewayService  = manaPayGatewayService;
    }

    // ─── Create ─────────────────────────────────────────────────────────────────

    /**
     * Creates a new {@link PaymentOrder} with status {@code INITIATED}.
     *
     * <p>If an order with the same {@code idempotencyKey} already exists it is
     * returned as-is — the caller should check the returned status before
     * re-directing the payer to the PG.
     */
    @Transactional
    public PaymentOrder createOrder(CreatePaymentOrderRequest req) {
        // Guard: gateway must be enabled for this community
        if (req.getGateway() != null && req.getCommunityId() != null) {
            paymentConfigService.assertGatewayAllowed(req.getCommunityId(), req.getGateway().name());
        }
        return repository.findByIdempotencyKey(req.getIdempotencyKey())
            .orElseGet(() -> {
                PaymentOrder order = PaymentOrder.builder()
                    .communityId(req.getCommunityId())
                    .idempotencyKey(req.getIdempotencyKey())
                    .gateway(req.getGateway())
                    .referenceType(req.getReferenceType())
                    .referenceId(req.getReferenceId())
                    .payerUserId(req.getPayerUserId())
                    .amount(req.getAmount())
                    .currency(req.getCurrency() != null ? req.getCurrency() : "INR")
                    .status(PaymentOrderStatus.INITIATED)
                    .refundedAmount(BigDecimal.ZERO)
                    .build();
                PaymentOrder saved = repository.save(order);
                log.info("[PaymentOrder] Created id={} key={} gateway={} ref={}/{}",
                    saved.getId(), saved.getIdempotencyKey(),
                    saved.getGateway(), saved.getReferenceType(), saved.getReferenceId());
                return saved;
            });
    }

    // ─── Gateway order ID bind ───────────────────────────────────────────────────

    /**
     * Stores the gateway's own order identifier after the PG call succeeds.
     * Transitions status from INITIATED to PENDING.
     */
    @Transactional
    public PaymentOrder bindGatewayOrder(String idempotencyKey, String gatewayOrderId) {
        PaymentOrder order = findByKeyOrThrow(idempotencyKey);
        stateMachine.assertTransition(order.getStatus(), PaymentOrderStatus.PENDING);
        order.setGatewayOrderId(gatewayOrderId);
        order.setStatus(PaymentOrderStatus.PENDING);
        return repository.save(order);
    }

    // ─── Success ─────────────────────────────────────────────────────────────────

    /**
     * Marks the order as {@code SUCCESS} after the gateway confirms capture.
     * Called from webhook handlers — never from the frontend.
     */
    @Transactional
    public PaymentOrder markSuccess(String idempotencyKey,
                                    String gatewayPaymentId,
                                    String gatewayOrderId) {
        PaymentOrder order = findByKeyOrThrow(idempotencyKey);
        stateMachine.assertTransition(order.getStatus(), PaymentOrderStatus.SUCCESS);
        order.setGatewayPaymentId(gatewayPaymentId);
        if (gatewayOrderId != null && order.getGatewayOrderId() == null) {
            order.setGatewayOrderId(gatewayOrderId);
        }
        order.setStatus(PaymentOrderStatus.SUCCESS);
        order.setWebhookReceivedAt(OffsetDateTime.now());
        PaymentOrder saved = repository.save(order);
        log.info("[PaymentOrder] SUCCESS id={} paymentId={}", saved.getId(), gatewayPaymentId);
        return saved;
    }

    /**
     * Variant that looks up by gateway + gatewayOrderId when the idempotency key
     * is not available (e.g. some legacy webhook payloads).
     */
    @Transactional
    public PaymentOrder markSuccessByGatewayOrder(PaymentGateway gateway,
                                                   String gatewayOrderId,
                                                   String gatewayPaymentId) {
        PaymentOrder order = repository.findByGatewayAndGatewayOrderId(gateway, gatewayOrderId)
            .orElseThrow(() -> new IllegalArgumentException(
                "No PaymentOrder found for " + gateway + " orderId=" + gatewayOrderId));
        stateMachine.assertTransition(order.getStatus(), PaymentOrderStatus.SUCCESS);
        order.setGatewayPaymentId(gatewayPaymentId);
        order.setStatus(PaymentOrderStatus.SUCCESS);
        order.setWebhookReceivedAt(OffsetDateTime.now());
        return repository.save(order);
    }

    // ─── Failure ─────────────────────────────────────────────────────────────────

    @Transactional
    public PaymentOrder markFailed(String idempotencyKey, String failureReason) {
        PaymentOrder order = findByKeyOrThrow(idempotencyKey);
        stateMachine.assertTransition(order.getStatus(), PaymentOrderStatus.FAILED);
        order.setStatus(PaymentOrderStatus.FAILED);
        order.setFailureReason(failureReason);
        order.setWebhookReceivedAt(OffsetDateTime.now());
        PaymentOrder saved = repository.save(order);
        log.warn("[PaymentOrder] FAILED id={} reason={}", saved.getId(), failureReason);
        return saved;
    }

    @Transactional
    public PaymentOrder markExpired(String idempotencyKey) {
        PaymentOrder order = findByKeyOrThrow(idempotencyKey);
        stateMachine.assertTransition(order.getStatus(), PaymentOrderStatus.EXPIRED);
        order.setStatus(PaymentOrderStatus.EXPIRED);
        order.setWebhookReceivedAt(OffsetDateTime.now());
        return repository.save(order);
    }

    // ─── Refund ──────────────────────────────────────────────────────────────────

    @Transactional
    public PaymentOrder initiateRefund(Long orderId, BigDecimal refundAmount) {
        PaymentOrder order = repository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("PaymentOrder not found: " + orderId));
        stateMachine.assertTransition(order.getStatus(), PaymentOrderStatus.REFUND_PENDING);
        if (refundAmount.compareTo(order.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount exceeds order amount");
        }
        order.setStatus(PaymentOrderStatus.REFUND_PENDING);
        log.info("[PaymentOrder] REFUND_PENDING id={} amount={}", orderId, refundAmount);
        return repository.save(order);
    }

    @Transactional
    public PaymentOrder markRefunded(Long orderId, BigDecimal refundedAmount) {
        PaymentOrder order = repository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("PaymentOrder not found: " + orderId));
        BigDecimal totalRefunded = order.getRefundedAmount().add(refundedAmount);
        if (totalRefunded.compareTo(order.getAmount()) > 0) {
            throw new IllegalArgumentException("Total refunded amount would exceed order amount");
        }

        // For ManaPay, credit the wallet back before recording the refund status
        if (order.getGateway() == PaymentGateway.MANA_PAY && order.getGatewayOrderId() != null) {
            manaPayGatewayService.refundPayment(
                order.getGatewayOrderId(), order.getPayerUserId(), refundedAmount, "Refund orderId=" + orderId);
        }

        order.setRefundedAmount(totalRefunded);
        PaymentOrderStatus next = totalRefunded.compareTo(order.getAmount()) == 0
            ? PaymentOrderStatus.REFUNDED
            : PaymentOrderStatus.PARTIALLY_REFUNDED;
        stateMachine.assertTransition(order.getStatus(), next);
        order.setStatus(next);
        log.info("[PaymentOrder] {} id={} refundedTotal={}", next, orderId, totalRefunded);
        return repository.save(order);
    }

    // ─── Ledger / receipt binding ────────────────────────────────────────────────

    @Transactional
    public void bindLedgerEntry(Long orderId, Long ledgerEntryId, String receiptNumber) {
        repository.findById(orderId).ifPresent(order -> {
            order.setLedgerEntryId(ledgerEntryId);
            order.setReceiptNumber(receiptNumber);
            repository.save(order);
        });
    }

    // ─── ManaPay (wallet) payment ────────────────────────────────────────────────

    /**
     * Initiates a ManaPay wallet payment for an existing order in INITIATED state.
     *
     * <p>ManaPay is synchronous — wallet debit happens immediately. If the debit
     * succeeds the order is set to SUCCESS and GatewayPaymentCapturedEvent is
     * published so ModulePaymentConfirmationListener updates the module entity.
     *
     * <p>Financial rule: amount comes from the stored PaymentOrder — never from
     * the caller. The order must already exist (created via createOrder).
     *
     * @throws InsufficientWalletBalanceException (402) if wallet balance insufficient
     */
    @Transactional
    public PaymentOrder initiateManaPayOrder(Long orderId) {
        PaymentOrder order = repository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("PaymentOrder not found: " + orderId));

        if (order.getGateway() != PaymentGateway.MANA_PAY) {
            throw new IllegalArgumentException(
                "Order " + orderId + " is not a MANA_PAY order (gateway=" + order.getGateway() + ")");
        }

        stateMachine.assertTransition(order.getStatus(), PaymentOrderStatus.PENDING);
        order.setStatus(PaymentOrderStatus.PENDING);

        String description = "ManaPay: " + order.getReferenceType() + " #" + order.getReferenceId();
        String manaRef = manaPayGatewayService.initiatePayment(
            order.getPayerUserId(), order.getCommunityId(), order.getAmount(), description);

        order.setGatewayOrderId(manaRef);

        boolean verified = manaPayGatewayService.verifyPayment(manaRef);
        if (verified) {
            stateMachine.assertTransition(PaymentOrderStatus.PENDING, PaymentOrderStatus.SUCCESS);
            order.setStatus(PaymentOrderStatus.SUCCESS);
            order.setGatewayPaymentId(manaRef);
            order.setWebhookReceivedAt(OffsetDateTime.now());
            PaymentOrder saved = repository.save(order);
            log.info("[ManaPay] SUCCESS orderId={} ref={}", orderId, manaRef);

            eventPublisher.publishEvent(new GatewayPaymentCapturedEvent(
                GatewayPaymentCapturedEvent.Gateway.MANA_PAY,
                manaRef,
                manaRef,
                order.getAmount(),
                order.getCurrency(),
                description,
                saved.getId()));

            return saved;
        } else {
            stateMachine.assertTransition(PaymentOrderStatus.PENDING, PaymentOrderStatus.FAILED);
            order.setStatus(PaymentOrderStatus.FAILED);
            order.setFailureReason("ManaPay wallet debit verification failed");
            order.setWebhookReceivedAt(OffsetDateTime.now());
            log.warn("[ManaPay] FAILED orderId={} ref={}", orderId, manaRef);
            return repository.save(order);
        }
    }

    // ─── Manual payment ───────────────────────────────────────────────────────────

    /**
     * Records an offline payment (cash/cheque/UPI) made directly to the community.
     * Creates a PaymentOrder with gateway=MANUAL and status=SUCCESS immediately,
     * then fires GatewayPaymentCapturedEvent so ModulePaymentConfirmationListener
     * updates the originating module entity.
     *
     * <p>Financial rule: amount is taken from the request but must be validated
     * against the expected amount of the referenced entity by the caller service.
     */
    @Transactional
    public PaymentOrder recordManualPayment(RecordManualPaymentRequest req, Long adminUserId) {
        String idempotencyKey = "MANUAL-" + req.getReferenceType()
            + "-" + req.getReferenceId() + "-" + UUID.randomUUID();

        String receiptNumber = "RCPT-MNL-" + System.currentTimeMillis();

        PaymentOrder order = PaymentOrder.builder()
            .communityId(req.getCommunityId())
            .idempotencyKey(idempotencyKey)
            .gateway(PaymentGateway.MANUAL)
            .referenceType(req.getReferenceType())
            .referenceId(req.getReferenceId())
            .payerUserId(adminUserId)
            .amount(req.getAmount())
            .currency("INR")
            .status(PaymentOrderStatus.SUCCESS)
            .refundedAmount(BigDecimal.ZERO)
            .receiptNumber(receiptNumber)
            .webhookReceivedAt(OffsetDateTime.now())
            .build();

        PaymentOrder saved = repository.save(order);
        log.info("[PaymentOrder] MANUAL recorded id={} ref={}/{} by adminId={} mode={}",
            saved.getId(), req.getReferenceType(), req.getReferenceId(),
            adminUserId, req.getPaymentMode());

        // Fire event so ModulePaymentConfirmationListener marks the module entity PAID
        eventPublisher.publishEvent(new GatewayPaymentCapturedEvent(
            GatewayPaymentCapturedEvent.Gateway.MANUAL,
            idempotencyKey,
            receiptNumber,
            req.getAmount(),
            "INR",
            "Manual payment: " + req.getPaymentMode()
                + (req.getReferenceNumber() != null ? " ref=" + req.getReferenceNumber() : ""),
            saved.getId()
        ));

        return saved;
    }

    // ─── Query ───────────────────────────────────────────────────────────────────

    public Optional<PaymentOrder> findByIdempotencyKey(String key) {
        return repository.findByIdempotencyKey(key);
    }

    public Optional<PaymentOrder> findByGatewayAndGatewayOrderId(PaymentGateway gateway,
                                                                   String gatewayOrderId) {
        return repository.findByGatewayAndGatewayOrderId(gateway, gatewayOrderId);
    }

    // ─── Internal ────────────────────────────────────────────────────────────────

    private PaymentOrder findByKeyOrThrow(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey)
            .orElseThrow(() -> new IllegalArgumentException(
                "No PaymentOrder found for idempotencyKey=" + idempotencyKey));
    }
}
