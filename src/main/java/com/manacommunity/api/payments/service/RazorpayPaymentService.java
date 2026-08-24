package com.manacommunity.api.payments.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.payments.config.RazorpayProperties;
import com.manacommunity.api.payments.dto.CreateOrderRequest;
import com.manacommunity.api.payments.dto.PaymentOrderResponse;
import com.manacommunity.api.payments.dto.VerifyPaymentRequest;
import com.manacommunity.api.payments.entity.RazorpayOrder;
import com.manacommunity.api.payments.exception.PaymentGatewayException;
import com.manacommunity.api.payments.exception.PaymentVerificationException;
import com.manacommunity.api.payments.repository.RazorpayOrderRepository;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.user.model.AppUser;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Central payment service for all Razorpay interactions.
 *
 * <p>This service is the ONLY place in the application that talks to the
 * Razorpay SDK. All business modules (events, pooja, donations, food, auction,
 * sports, etc.) call this service — never Razorpay directly.
 *
 * <p>Key design principles:
 * <ul>
 *   <li>The frontend NEVER determines payment success — the backend verifies
 *       the HMAC-SHA256 signature before marking an order as PAID.</li>
 *   <li>Amounts are stored in INR rupees (BigDecimal 12,2) and converted to
 *       paise (×100) only at the Razorpay API call boundary.</li>
 *   <li>Razorpay {@code keySecret} and {@code webhookSecret} are never exposed
 *       outside this service.</li>
 *   <li>Webhook processing is idempotent via {@code webhook_event_id}.</li>
 *   <li>@ConditionalOnProperty ensures this bean is absent when Razorpay is
 *       disabled — the entire payment stack stays dormant.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.razorpay.enabled", havingValue = "true")
public class RazorpayPaymentService {

    private final RazorpayOrderRepository orderRepository;
    private final RazorpayProperties      properties;
    private final RazorpayClient          razorpayClient;
    private final AuditService            auditService;

    // ─── Order Creation ──────────────────────────────────────────────────────

    /**
     * Creates a Razorpay order and persists an internal {@link RazorpayOrder} row.
     *
     * <p>Transaction boundary: the internal record is saved BEFORE calling
     * Razorpay so we have an audit trail even if the gateway call fails.
     * If Razorpay fails, the record status stays "CREATED" and can be retried.
     *
     * @param req       validated request from the frontend
     * @param user      the authenticated user
     * @param community the user's community
     * @return response containing the Razorpay order ID and publishable keyId
     */
    @Transactional
    public PaymentOrderResponse createOrder(CreateOrderRequest req, AppUser user, Community community) {
        // Convert rupees to paise (Razorpay smallest unit for INR).
        long amountInPaise = req.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", properties.getCurrency());
        // receipt ties the Razorpay order back to our user/time for support queries.
        orderRequest.put("receipt", "mana_" + user.getId() + "_" + System.currentTimeMillis());
        orderRequest.put("payment_capture", 1); // auto-capture immediately on success

        JSONObject notes = new JSONObject();
        notes.put("userId", user.getId());
        notes.put("communityId", community.getId());
        notes.put("referenceType", req.getReferenceType());
        notes.put("referenceId", req.getReferenceId());
        orderRequest.put("notes", notes);

        try {
            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            RazorpayOrder entity = RazorpayOrder.builder()
                    .razorpayOrderId(razorpayOrderId)
                    .amount(req.getAmount())
                    .currency(properties.getCurrency())
                    .status("CREATED")
                    .referenceType(req.getReferenceType())
                    .referenceId(req.getReferenceId())
                    .description(req.getDescription())
                    .notes(notes.toString())
                    .user(user)
                    .community(community)
                    .build();

            RazorpayOrder saved = orderRepository.save(entity);

            auditService.record(
                    AuditAction.PAYMENT_ORDER_CREATED,
                    AuditModule.PAYMENT,
                    "RazorpayOrder",
                    String.valueOf(saved.getId())
            );

            log.info("Razorpay order created: razorpayOrderId={} amount=₹{} userId={} communityId={} ref={}:{}",
                    razorpayOrderId, req.getAmount(), user.getId(), community.getId(),
                    req.getReferenceType(), req.getReferenceId());

            return toResponse(saved, true);

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for userId={} ref={}:{}: {}",
                    user.getId(), req.getReferenceType(), req.getReferenceId(), e.getMessage(), e);
            throw new PaymentGatewayException("Payment gateway error. Please try again.", e);
        }
    }

    // ─── Payment Verification ────────────────────────────────────────────────

    /**
     * Verifies the Razorpay payment signature after the frontend completes checkout.
     *
     * <p>This method is idempotent: if the order is already PAID it returns the
     * current state without re-processing.
     *
     * @param req the three Razorpay IDs from the checkout success handler
     * @return updated payment order response
     * @throws PaymentVerificationException if the signature is invalid
     */
    @Transactional
    public PaymentOrderResponse verifyPayment(VerifyPaymentRequest req) {
        RazorpayOrder order = orderRepository.findByRazorpayOrderId(req.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment order", "razorpayOrderId", req.getRazorpayOrderId()));

        // Idempotent: already PAID — return current state without re-processing.
        if ("PAID".equals(order.getStatus())) {
            log.info("Duplicate verify attempt for already-PAID order: {}", req.getRazorpayOrderId());
            return toResponse(order, false);
        }

        boolean valid = verifyPaymentSignature(
                req.getRazorpayOrderId(),
                req.getRazorpayPaymentId(),
                req.getRazorpaySignature());

        if (!valid) {
            log.warn("SECURITY: Razorpay signature mismatch for razorpayOrderId={} paymentId={}",
                    req.getRazorpayOrderId(), req.getRazorpayPaymentId());

            String prevStatus = order.getStatus();
            order.setStatus("FAILED");
            order.setFailureReason("Signature verification failed");
            orderRepository.save(order);

            auditService.record(
                    AuditAction.PAYMENT_VERIFICATION_FAILED,
                    AuditModule.PAYMENT,
                    "RazorpayOrder",
                    String.valueOf(order.getId()),
                    prevStatus, "FAILED"
            );
            throw new PaymentVerificationException(
                    "Payment verification failed. If money was deducted, it will be automatically refunded.");
        }

        String prevStatus = order.getStatus();
        order.setRazorpayPaymentId(req.getRazorpayPaymentId());
        order.setRazorpaySignature(req.getRazorpaySignature());
        order.setStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        RazorpayOrder saved = orderRepository.save(order);

        auditService.record(
                AuditAction.PAYMENT_COMPLETED,
                AuditModule.PAYMENT,
                "RazorpayOrder",
                String.valueOf(saved.getId()),
                prevStatus, "PAID"
        );

        log.info("Payment verified PAID: razorpayOrderId={} paymentId={} amount=₹{}",
                req.getRazorpayOrderId(), req.getRazorpayPaymentId(), saved.getAmount());

        return toResponse(saved, false);
    }

    // ─── Webhook Processing ──────────────────────────────────────────────────

    /**
     * Processes an incoming Razorpay webhook event.
     *
     * <p>Called by the controller without JWT authentication (Razorpay is the
     * caller). Authenticity is enforced by HMAC-SHA256 verification of the
     * {@code X-Razorpay-Signature} header against the raw request body.
     *
     * <p>Processing is idempotent: if {@code event.id} has already been seen,
     * the event is silently skipped. The controller always returns HTTP 200
     * quickly so Razorpay does not consider the delivery failed.
     *
     * @param payload   raw JSON body from Razorpay
     * @param signature value of {@code X-Razorpay-Signature} header
     * @throws PaymentVerificationException if the webhook signature is invalid
     */
    @Transactional
    public void processWebhook(String payload, String signature) {
        auditService.record(AuditAction.PAYMENT_WEBHOOK_RECEIVED,
                AuditModule.PAYMENT, "RazorpayWebhook", "incoming");

        if (!verifyWebhookSignature(payload, signature)) {
            log.warn("SECURITY: Razorpay webhook rejected — signature mismatch");
            throw new PaymentVerificationException("Webhook signature verification failed");
        }

        JSONObject event    = new JSONObject(payload);
        String     eventType = event.optString("event", "");
        String     eventId   = event.optString("id",    "");

        // Idempotency — skip if already processed.
        if (!eventId.isBlank() && orderRepository.existsByWebhookEventId(eventId)) {
            log.info("Skipping duplicate Razorpay webhook eventId={}", eventId);
            return;
        }

        log.info("Processing Razorpay webhook: event={} eventId={}", eventType, eventId);

        switch (eventType) {
            case "payment.captured" -> handlePaymentCaptured(event, eventId);
            case "payment.failed"   -> handlePaymentFailed(event, eventId);
            case "refund.processed" -> handleRefundProcessed(event, eventId);
            default -> log.debug("Unhandled Razorpay webhook event type: {}", eventType);
        }
    }

    private void handlePaymentCaptured(JSONObject event, String eventId) {
        JSONObject entity        = event.getJSONObject("payload")
                                       .getJSONObject("payment")
                                       .getJSONObject("entity");
        String razorpayOrderId   = entity.optString("order_id");
        String razorpayPaymentId = entity.optString("id");

        orderRepository.findByRazorpayOrderId(razorpayOrderId).ifPresentOrElse(order -> {
            if (!"PAID".equals(order.getStatus())) {
                String prev = order.getStatus();
                order.setRazorpayPaymentId(razorpayPaymentId);
                order.setStatus("PAID");
                order.setPaidAt(LocalDateTime.now());
                order.setWebhookEventId(eventId);
                orderRepository.save(order);

                auditService.record(AuditAction.PAYMENT_WEBHOOK_PROCESSED, AuditModule.PAYMENT,
                        "RazorpayOrder", String.valueOf(order.getId()), prev, "PAID");
                log.info("Webhook payment.captured: order {} PAID via paymentId={}", razorpayOrderId, razorpayPaymentId);
            }
        }, () -> log.warn("Webhook payment.captured: no local order found for razorpayOrderId={}", razorpayOrderId));
    }

    private void handlePaymentFailed(JSONObject event, String eventId) {
        JSONObject entity      = event.getJSONObject("payload")
                                     .getJSONObject("payment")
                                     .getJSONObject("entity");
        String razorpayOrderId = entity.optString("order_id");
        String errorDesc       = entity.optString("error_description", "Payment failed");

        orderRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(order -> {
            if ("CREATED".equals(order.getStatus())) {
                order.setStatus("FAILED");
                order.setFailureReason(errorDesc);
                order.setWebhookEventId(eventId);
                orderRepository.save(order);
                log.info("Webhook payment.failed: order {} FAILED: {}", razorpayOrderId, errorDesc);
            }
        });
    }

    private void handleRefundProcessed(JSONObject event, String eventId) {
        JSONObject entity        = event.getJSONObject("payload")
                                       .getJSONObject("refund")
                                       .getJSONObject("entity");
        String razorpayPaymentId = entity.optString("payment_id");

        orderRepository.findByRazorpayPaymentId(razorpayPaymentId).ifPresent(order -> {
            String prev = order.getStatus();
            order.setStatus("REFUNDED");
            order.setWebhookEventId(eventId);
            orderRepository.save(order);

            auditService.record(AuditAction.PAYMENT_REFUNDED, AuditModule.PAYMENT,
                    "RazorpayOrder", String.valueOf(order.getId()), prev, "REFUNDED");
            log.info("Webhook refund.processed: order {} REFUNDED for paymentId={}",
                    order.getId(), razorpayPaymentId);
        });
    }

    // ─── Queries ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PaymentOrderResponse> getMyOrders(Long userId, Long communityId, Pageable pageable) {
        return orderRepository.findByUserIdAndCommunityId(userId, communityId, pageable)
                .map(o -> toResponse(o, false));
    }

    @Transactional(readOnly = true)
    public Page<PaymentOrderResponse> getCommunityOrders(Long communityId, Pageable pageable) {
        return orderRepository.findByCommunityId(communityId, pageable)
                .map(o -> toResponse(o, false));
    }

    @Transactional(readOnly = true)
    public PaymentOrderResponse getById(Long id, Long communityId) {
        RazorpayOrder order = orderRepository.findById(id)
                .filter(o -> o.getCommunity().getId().equals(communityId))
                .orElseThrow(() -> new ResourceNotFoundException("Payment order", id));
        return toResponse(order, false);
    }

    @Transactional(readOnly = true)
    public Page<PaymentOrderResponse> getByReference(
            String referenceType, Long referenceId, Long communityId, Pageable pageable) {
        return orderRepository.findByReferenceTypeAndReferenceId(referenceType, referenceId, pageable)
                .map(o -> toResponse(o, false));
    }

    // ─── Signature Helpers ───────────────────────────────────────────────────

    private boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id",   orderId);
            attributes.put("razorpay_payment_id",  paymentId);
            attributes.put("razorpay_signature",   signature);
            Utils.verifyPaymentSignature(attributes, properties.getKeySecret());
            return true;
        } catch (RazorpayException e) {
            log.debug("Payment signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean verifyWebhookSignature(String payload, String signature) {
        try {
            Utils.verifyWebhookSignature(payload, signature, properties.getWebhookSecret());
            return true;
        } catch (RazorpayException e) {
            log.debug("Webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    private PaymentOrderResponse toResponse(RazorpayOrder order, boolean includeKeyId) {
        return PaymentOrderResponse.builder()
                .id(order.getId())
                .razorpayOrderId(order.getRazorpayOrderId())
                .razorpayPaymentId(order.getRazorpayPaymentId())
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .status(order.getStatus())
                .referenceType(order.getReferenceType())
                .referenceId(order.getReferenceId())
                .description(order.getDescription())
                // keyId is the PUBLISHABLE Razorpay key — safe to expose.
                // Returned only on order creation so the frontend can open the modal.
                .keyId(includeKeyId ? properties.getKeyId() : null)
                .paidAt(order.getPaidAt())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
