package com.manacommunity.api.payments.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.payments.config.CashfreeProperties;
import com.manacommunity.api.payments.dto.CashfreeCreateOrderRequest;
import com.manacommunity.api.payments.dto.CashfreeOrderResponse;
import com.manacommunity.api.payments.entity.CashfreeOrder;
import com.manacommunity.api.payments.exception.PaymentGatewayException;
import com.manacommunity.api.payments.exception.PaymentVerificationException;
import com.manacommunity.api.payments.repository.CashfreeOrderRepository;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Central payment service for all Cashfree PG interactions.
 *
 * <p>This service is the ONLY place that talks to the Cashfree REST API.
 * Business modules (sports, events, venue booking, marketplace) call this
 * service — never Cashfree directly.
 *
 * <p>Key design principles:
 * <ul>
 *   <li>The frontend NEVER determines payment success — the backend re-queries
 *       Cashfree's order-payments API before marking an order as PAID.</li>
 *   <li>Amounts are stored in INR rupees (BigDecimal 12,2).</li>
 *   <li>Cashfree secret key and webhook secret are never exposed outside
 *       this service.</li>
 *   <li>Webhook processing is idempotent via {@code webhook_event_id}.</li>
 *   <li>@ConditionalOnProperty keeps this bean absent when disabled.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.cashfree.enabled", havingValue = "true")
public class CashfreePaymentService {

    private final CashfreeOrderRepository orderRepository;
    private final CashfreeProperties      properties;
    @Qualifier("cashfreeRestTemplate")
    private final RestTemplate            restTemplate;
    private final AuditService            auditService;

    // ─── Order Creation ──────────────────────────────────────────────────────

    /**
     * Creates a Cashfree order and persists an internal {@link CashfreeOrder} row.
     *
     * <p>The internal record is saved BEFORE calling Cashfree so there is always
     * an audit trail. If the gateway call fails the record status stays "CREATED"
     * and can be retried.
     */
    @Transactional
    public CashfreeOrderResponse createOrder(
            CashfreeCreateOrderRequest req, AppUser user, Community community) {

        String manaOrderId = "MANA-" + System.currentTimeMillis() + "-" + user.getId();

        // Persist the local record first.
        CashfreeOrder entity = CashfreeOrder.builder()
                .manaOrderId(manaOrderId)
                .amount(req.getAmount())
                .currency(properties.getCurrency())
                .status("CREATED")
                .referenceType(req.getReferenceType())
                .referenceId(req.getReferenceId())
                .description(req.getDescription())
                .user(user)
                .community(community)
                .build();
        CashfreeOrder saved = orderRepository.save(entity);

        // Build Cashfree order-creation payload.
        Map<String, Object> customerDetails = new HashMap<>();
        customerDetails.put("customer_id", "mana_user_" + user.getId());
        customerDetails.put("customer_name", user.getFullName() != null ? user.getFullName() : user.getName());
        customerDetails.put("customer_email", user.getEmail());
        customerDetails.put("customer_phone",
                req.getCustomerPhone() != null ? req.getCustomerPhone() : "9999999999");

        Map<String, Object> orderMeta = new HashMap<>();
        orderMeta.put("return_url",
                "mana://payment/return?order_id=" + manaOrderId);
        orderMeta.put("notify_url",
                "https://api.manacommunityapp.com/api/payments/cashfree/webhook");

        Map<String, Object> body = new HashMap<>();
        body.put("order_id", manaOrderId);
        body.put("order_amount", req.getAmount());
        body.put("order_currency", properties.getCurrency());
        body.put("customer_details", customerDetails);
        body.put("order_meta", orderMeta);
        if (req.getDescription() != null) {
            body.put("order_note", req.getDescription());
        }

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    properties.getBaseUrl() + "/orders",
                    HttpMethod.POST,
                    new HttpEntity<>(body, buildHeaders()),
                    Map.class
            );

            Map<?, ?> responseBody = response.getBody();
            if (responseBody == null) {
                throw new PaymentGatewayException("Empty response from Cashfree.");
            }

            Object cfOrderIdRaw        = responseBody.get("cf_order_id");
            Object paymentSessionIdRaw = responseBody.get("payment_session_id");
            String cfOrderId        = cfOrderIdRaw        != null ? String.valueOf(cfOrderIdRaw)        : "";
            String paymentSessionId = paymentSessionIdRaw != null ? String.valueOf(paymentSessionIdRaw) : "";

            saved.setCfOrderId(cfOrderId);
            saved.setPaymentSessionId(paymentSessionId);
            saved = orderRepository.save(saved);

            auditService.record(AuditAction.PAYMENT_ORDER_CREATED, AuditModule.PAYMENT,
                    "CashfreeOrder", String.valueOf(saved.getId()));

            log.info("Cashfree order created: manaOrderId={} cfOrderId={} amount=₹{} userId={} ref={}:{}",
                    manaOrderId, cfOrderId, req.getAmount(), user.getId(),
                    req.getReferenceType(), req.getReferenceId());

            return toResponse(saved, true);

        } catch (RestClientException e) {
            log.error("Failed to create Cashfree order for userId={} ref={}:{}: {}",
                    user.getId(), req.getReferenceType(), req.getReferenceId(), e.getMessage(), e);
            throw new PaymentGatewayException("Payment gateway error. Please try again.", e);
        }
    }

    // ─── Payment Verification ────────────────────────────────────────────────

    /**
     * Verifies the payment status by re-querying Cashfree's payments API.
     *
     * <p>This method is idempotent: if the order is already PAID it returns
     * the current state without re-processing.
     *
     * <p>Never trust the mobile SDK's onVerify orderId alone — always re-query
     * Cashfree to confirm the authoritative status.
     */
    @Transactional
    public CashfreeOrderResponse verifyPayment(String manaOrderId) {
        CashfreeOrder order = orderRepository.findByManaOrderId(manaOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment order", "manaOrderId", manaOrderId));

        if ("PAID".equals(order.getStatus())) {
            log.info("Duplicate verify for already-PAID order: {}", manaOrderId);
            return toResponse(order, false);
        }

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    properties.getBaseUrl() + "/orders/" + manaOrderId + "/payments",
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    Map.class
            );

            Map<?, ?> body = response.getBody();
            if (body == null) {
                throw new PaymentVerificationException("Empty verification response from Cashfree.");
            }

            // Cashfree returns an array; the payment_status field shows SUCCESS/FAILED/PENDING.
            String paymentStatus  = extractPaymentStatus(body);
            String cfPaymentId    = extractPaymentId(body);
            String paymentMethod  = extractPaymentMethod(body);

            if ("SUCCESS".equals(paymentStatus)) {
                String prev = order.getStatus();
                order.setCfPaymentId(cfPaymentId);
                order.setPaymentMethod(paymentMethod);
                order.setStatus("PAID");
                order.setPaidAt(LocalDateTime.now());
                orderRepository.save(order);
                auditService.record(AuditAction.PAYMENT_COMPLETED, AuditModule.PAYMENT,
                        "CashfreeOrder", String.valueOf(order.getId()), prev, "PAID");
                log.info("Payment verified PAID: manaOrderId={} cfPaymentId={} amount=₹{}",
                        manaOrderId, cfPaymentId, order.getAmount());
                return toResponse(order, false);

            } else if ("FAILED".equals(paymentStatus)) {
                order.setStatus("FAILED");
                order.setFailureReason("Payment failed at gateway.");
                orderRepository.save(order);
                throw new PaymentVerificationException("Payment failed. Please try again.");

            } else {
                // PENDING or unknown — return current status.
                return toResponse(order, false);
            }

        } catch (RestClientException e) {
            log.error("Failed to verify Cashfree order {}: {}", manaOrderId, e.getMessage(), e);
            throw new PaymentGatewayException("Verification request failed. Please contact support.", e);
        }
    }

    // ─── Webhook Processing ──────────────────────────────────────────────────

    /**
     * Processes an incoming Cashfree webhook event.
     *
     * <p>Authenticity is enforced by HMAC-SHA256 verification of
     * {@code x-webhook-signature} against (timestamp + rawBody).
     * Processing is idempotent via webhook_event_id.
     */
    @Transactional
    public void processWebhook(String payload, String signature, String timestamp) {
        auditService.record(AuditAction.PAYMENT_WEBHOOK_RECEIVED,
                AuditModule.PAYMENT, "CashfreeWebhook", "incoming");

        if (!verifyWebhookSignature(payload, signature, timestamp)) {
            log.warn("SECURITY: Cashfree webhook rejected — signature mismatch");
            throw new PaymentVerificationException("Webhook signature verification failed");
        }

        JSONObject event     = new JSONObject(payload);
        String eventType     = event.optString("type", "");
        // Cashfree uses the event's cf_payment_id or order_id as a deduplication key.
        String eventId       = event.optString("event_time", "") + "_" + eventType
                             + "_" + event.optJSONObject("data") != null
                               ? extractEventDedupeKey(event) : eventType;

        if (!eventId.isBlank() && orderRepository.existsByWebhookEventId(eventId)) {
            log.info("Skipping duplicate Cashfree webhook eventId={}", eventId);
            return;
        }

        log.info("Processing Cashfree webhook: type={}", eventType);

        switch (eventType) {
            case "PAYMENT_SUCCESS" -> handlePaymentSuccess(event, eventId);
            case "PAYMENT_FAILED"  -> handlePaymentFailed(event, eventId);
            case "PAYMENT_USER_DROPPED" -> handlePaymentDropped(event, eventId);
            case "REFUND_STATUS_WEBHOOK" -> handleRefund(event, eventId);
            default -> log.debug("Unhandled Cashfree webhook type: {}", eventType);
        }
    }

    private void handlePaymentSuccess(JSONObject event, String eventId) {
        JSONObject data     = event.optJSONObject("data");
        if (data == null) return;
        JSONObject orderObj = data.optJSONObject("order");
        JSONObject payObj   = data.optJSONObject("payment");
        if (orderObj == null || payObj == null) return;

        String manaOrderId  = orderObj.optString("order_id");
        String cfPaymentId  = payObj.optString("cf_payment_id", payObj.optString("bank_reference", ""));
        String paymentMethod = payObj.optString("payment_group", "");

        orderRepository.findByManaOrderId(manaOrderId).ifPresentOrElse(order -> {
            if (!"PAID".equals(order.getStatus())) {
                String prev = order.getStatus();
                order.setCfPaymentId(cfPaymentId);
                order.setPaymentMethod(paymentMethod);
                order.setStatus("PAID");
                order.setPaidAt(LocalDateTime.now());
                order.setWebhookEventId(eventId);
                orderRepository.save(order);
                auditService.record(AuditAction.PAYMENT_WEBHOOK_PROCESSED, AuditModule.PAYMENT,
                        "CashfreeOrder", String.valueOf(order.getId()), prev, "PAID");
                log.info("Webhook PAYMENT_SUCCESS: order {} PAID via cfPaymentId={}", manaOrderId, cfPaymentId);
            }
        }, () -> log.warn("Webhook PAYMENT_SUCCESS: no local order for manaOrderId={}", manaOrderId));
    }

    private void handlePaymentFailed(JSONObject event, String eventId) {
        JSONObject data    = event.optJSONObject("data");
        if (data == null) return;
        JSONObject orderObj = data.optJSONObject("order");
        JSONObject errObj   = data.optJSONObject("error_details");
        if (orderObj == null) return;

        String manaOrderId = orderObj.optString("order_id");
        String errorDesc   = errObj != null
                           ? errObj.optString("error_description", "Payment failed")
                           : "Payment failed";

        orderRepository.findByManaOrderId(manaOrderId).ifPresent(order -> {
            if ("CREATED".equals(order.getStatus()) || "PENDING".equals(order.getStatus())) {
                order.setStatus("FAILED");
                order.setFailureReason(errorDesc);
                order.setWebhookEventId(eventId);
                orderRepository.save(order);
                log.info("Webhook PAYMENT_FAILED: order {} FAILED: {}", manaOrderId, errorDesc);
            }
        });
    }

    private void handlePaymentDropped(JSONObject event, String eventId) {
        // User closed checkout — leave status as CREATED so they can retry.
        log.debug("Cashfree PAYMENT_USER_DROPPED webhook received, no action.");
    }

    private void handleRefund(JSONObject event, String eventId) {
        JSONObject data   = event.optJSONObject("data");
        if (data == null) return;
        JSONObject refund = data.optJSONObject("refund");
        if (refund == null) return;

        String cfPaymentId    = refund.optString("cf_payment_id", "");
        String refundStatus   = refund.optString("refund_status", "");

        if ("SUCCESS".equals(refundStatus)) {
            orderRepository.findByCfPaymentId(cfPaymentId).ifPresent(order -> {
                String prev = order.getStatus();
                order.setStatus("REFUNDED");
                order.setWebhookEventId(eventId);
                orderRepository.save(order);
                auditService.record(AuditAction.PAYMENT_REFUNDED, AuditModule.PAYMENT,
                        "CashfreeOrder", String.valueOf(order.getId()), prev, "REFUNDED");
                log.info("Webhook refund SUCCESS: order {} REFUNDED for cfPaymentId={}", order.getId(), cfPaymentId);
            });
        }
    }

    // ─── Queries ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CashfreeOrderResponse> getMyOrders(Long userId, Long communityId, Pageable pageable) {
        return orderRepository.findByUserIdAndCommunityId(userId, communityId, pageable)
                .map(o -> toResponse(o, false));
    }

    @Transactional(readOnly = true)
    public Page<CashfreeOrderResponse> getCommunityOrders(Long communityId, Pageable pageable) {
        return orderRepository.findByCommunityId(communityId, pageable)
                .map(o -> toResponse(o, false));
    }

    @Transactional(readOnly = true)
    public CashfreeOrderResponse getById(Long id, Long communityId) {
        CashfreeOrder order = orderRepository.findById(id)
                .filter(o -> o.getCommunity().getId().equals(communityId))
                .orElseThrow(() -> new ResourceNotFoundException("Payment order", id));
        return toResponse(order, false);
    }

    @Transactional(readOnly = true)
    public Page<CashfreeOrderResponse> getByReference(
            String referenceType, Long referenceId, Long communityId, Pageable pageable) {
        return orderRepository.findByReferenceTypeAndReferenceId(referenceType, referenceId, pageable)
                .map(o -> toResponse(o, false));
    }

    // ─── Webhook Signature Verification ──────────────────────────────────────

    /**
     * Cashfree webhook signature: HMAC-SHA256(timestamp + "\n" + rawBody, secretKey),
     * then Base64-encoded.
     */
    boolean verifyWebhookSignature(String payload, String signature, String timestamp) {
        try {
            String signedPayload = timestamp + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmac     = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            String computed = Base64.getEncoder().encodeToString(hmac);
            return computed.equals(signature);
        } catch (Exception e) {
            log.debug("Webhook signature verification error: {}", e.getMessage());
            return false;
        }
    }

    // ─── HTTP Helpers ─────────────────────────────────────────────────────────

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-version", properties.getApiVersion());
        headers.set("x-client-id", properties.getAppId());
        headers.set("x-client-secret", properties.getSecretKey());
        return headers;
    }

    // ─── Cashfree Response Parsing ────────────────────────────────────────────

    /**
     * Cashfree GET /orders/{id}/payments returns a list; find the latest SUCCESS or
     * the first entry's status.
     */
    private String extractPaymentStatus(Map<?, ?> body) {
        // The response may be a direct object (single payment) or a list.
        // Cashfree v3 wraps results in data array in some versions.
        try {
            if (body.containsKey("payment_status")) {
                return String.valueOf(body.get("payment_status"));
            }
            if (body.containsKey("data")) {
                Object data = body.get("data");
                if (data instanceof java.util.List<?> list && !list.isEmpty()) {
                    Map<?, ?> first = (Map<?, ?>) list.get(0);
                    Object val = first.get("payment_status");
                    return val != null ? String.valueOf(val) : "PENDING";
                }
            }
        } catch (Exception e) {
            log.debug("Could not parse payment_status from Cashfree response: {}", e.getMessage());
        }
        return "PENDING";
    }

    private String extractPaymentId(Map<?, ?> body) {
        try {
            if (body.containsKey("cf_payment_id")) {
                return String.valueOf(body.get("cf_payment_id"));
            }
            if (body.containsKey("data")) {
                Object data = body.get("data");
                if (data instanceof java.util.List<?> list && !list.isEmpty()) {
                    Map<?, ?> first = (Map<?, ?>) list.get(0);
                    Object val = first.get("cf_payment_id");
                    return val != null ? String.valueOf(val) : "";
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String extractPaymentMethod(Map<?, ?> body) {
        try {
            if (body.containsKey("payment_group")) {
                return String.valueOf(body.get("payment_group"));
            }
            if (body.containsKey("data")) {
                Object data = body.get("data");
                if (data instanceof java.util.List<?> list && !list.isEmpty()) {
                    Map<?, ?> first = (Map<?, ?>) list.get(0);
                    Object val = first.get("payment_group");
                    return val != null ? String.valueOf(val) : "";
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String extractEventDedupeKey(JSONObject event) {
        try {
            JSONObject data = event.optJSONObject("data");
            if (data != null) {
                JSONObject payment = data.optJSONObject("payment");
                if (payment != null) {
                    return payment.optString("cf_payment_id", event.optString("event_time", ""));
                }
                JSONObject order = data.optJSONObject("order");
                if (order != null) {
                    return order.optString("order_id", event.optString("event_time", ""));
                }
            }
        } catch (Exception ignored) {}
        return event.optString("event_time", "unknown");
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    private CashfreeOrderResponse toResponse(CashfreeOrder order, boolean includeSessionId) {
        return CashfreeOrderResponse.builder()
                .id(order.getId())
                .manaOrderId(order.getManaOrderId())
                .cfOrderId(order.getCfOrderId())
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .status(order.getStatus())
                .referenceType(order.getReferenceType())
                .referenceId(order.getReferenceId())
                .description(order.getDescription())
                .paymentMethod(order.getPaymentMethod())
                .failureReason(order.getFailureReason())
                .paidAt(order.getPaidAt())
                .createdAt(order.getCreatedAt())
                // Only expose session credentials on order creation.
                .paymentSessionId(includeSessionId ? order.getPaymentSessionId() : null)
                .appId(includeSessionId ? properties.getAppId() : null)
                .build();
    }
}
