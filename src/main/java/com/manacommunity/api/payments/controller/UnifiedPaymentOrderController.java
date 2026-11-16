package com.manacommunity.api.payments.controller;

import com.manacommunity.api.payments.dto.CreatePaymentOrderRequest;
import com.manacommunity.api.payments.dto.UnifiedPaymentOrderResponse;
import com.manacommunity.api.payments.dto.VerifyPaymentRequest;
import com.manacommunity.api.payments.entity.PaymentOrder;
import com.manacommunity.api.payments.model.PaymentGateway;
import com.manacommunity.api.payments.model.PaymentOrderStatus;
import com.manacommunity.api.payments.model.PaymentReferenceType;
import com.manacommunity.api.payments.repository.PaymentOrderRepository;
import com.manacommunity.api.payments.service.CashfreePaymentService;
import com.manacommunity.api.payments.service.RazorpayPaymentService;
import com.manacommunity.api.payments.service.UnifiedPaymentOrderService;
import com.manacommunity.api.user.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Unified REST controller for all payment order operations.
 *
 * <p>All feature modules (maintenance, events, sports, food, marketplace,
 * health, amenities, trips) must create a payment order here before
 * redirecting to any payment gateway.
 *
 * <p>Endpoint layout:
 * <ul>
 *   <li>{@code POST /api/payments/orders}                       — create order</li>
 *   <li>{@code GET  /api/payments/orders/{id}}                  — get order by id</li>
 *   <li>{@code POST /api/payments/orders/{id}/verify}           — server-side gateway verify</li>
 *   <li>{@code POST /api/payments/orders/{id}/refund}           — initiate refund</li>
 *   <li>{@code GET  /api/payments/orders?referenceType=&referenceId=} — by reference</li>
 * </ul>
 *
 * <p>Community isolation: all endpoints reject requests where the authenticated
 * principal's communityId does not match the payment order's communityId.
 */
@Slf4j
@Tag(name = "Payments", description = "Unified payment order lifecycle")
@RestController
@RequestMapping("/api/payments/orders")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UnifiedPaymentOrderController {

    private final UnifiedPaymentOrderService orderService;
    private final PaymentOrderRepository     orderRepository;
    private final CashfreePaymentService     cashfreePaymentService;
    private final RazorpayPaymentService     razorpayPaymentService;

    // ─── Create ──────────────────────────────────────────────────────────────────

    @Operation(summary = "Create a payment order before launching PG SDK")
    @PostMapping
    public ResponseEntity<UnifiedPaymentOrderResponse> createOrder(
            @Valid @RequestBody CreatePaymentOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        // Community isolation: reject cross-community order creation
        if (!principal.getCommunityId().equals(request.getCommunityId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "communityId in request does not match authenticated user's community");
        }

        // Auto-generate idempotency key if caller omitted it
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            request.setIdempotencyKey(UUID.randomUUID().toString());
        }

        // Stamp the payer from the authenticated principal — never trust frontend
        request.setPayerUserId(principal.getUserId());

        PaymentOrder order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(UnifiedPaymentOrderResponse.from(order));
    }

    // ─── Get ─────────────────────────────────────────────────────────────────────

    @Operation(summary = "Get a payment order by id")
    @GetMapping("/{id}")
    public ResponseEntity<UnifiedPaymentOrderResponse> getOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        PaymentOrder order = loadAndAssertAccess(id, principal);
        return ResponseEntity.ok(UnifiedPaymentOrderResponse.from(order));
    }

    // ─── By reference ────────────────────────────────────────────────────────────

    @Operation(summary = "List payment orders for a business entity")
    @GetMapping
    public ResponseEntity<List<UnifiedPaymentOrderResponse>> getOrdersForReference(
            @RequestParam PaymentReferenceType referenceType,
            @RequestParam Long referenceId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<PaymentOrder> orders = orderRepository
            .findByReferenceTypeAndReferenceId(referenceType, referenceId);

        // Filter to this community only — community isolation
        List<UnifiedPaymentOrderResponse> result = orders.stream()
            .filter(o -> principal.getCommunityId().equals(o.getCommunityId()))
            .map(UnifiedPaymentOrderResponse::from)
            .toList();

        return ResponseEntity.ok(result);
    }

    // ─── Verify ──────────────────────────────────────────────────────────────────

    /**
     * Server-side gateway verification — MUST be called after the PG SDK callback.
     * Never accept the SDK result alone as payment confirmation.
     */
    @Operation(summary = "Verify payment status with the gateway (server-side)")
    @PostMapping("/{id}/verify")
    public ResponseEntity<UnifiedPaymentOrderResponse> verifyPayment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        PaymentOrder order = loadAndAssertAccess(id, principal);

        // Terminal states don't need re-verification
        if (order.getStatus().isTerminal()) {
            return ResponseEntity.ok(UnifiedPaymentOrderResponse.from(order));
        }

        try {
            if (order.getGateway() == PaymentGateway.CASHFREE) {
                verifyCashfree(order);
            } else if (order.getGateway() == PaymentGateway.RAZORPAY) {
                verifyRazorpay(order);
            }
        } catch (Exception e) {
            log.warn("[UnifiedPaymentOrderController] Verification failed for order id={}: {}",
                id, e.getMessage());
            // Don't surface gateway errors to the frontend — return current stored status
        }

        // Reload after potential status update
        return ResponseEntity.ok(UnifiedPaymentOrderResponse.from(
            orderRepository.findById(id).orElse(order)));
    }

    // ─── Refund ──────────────────────────────────────────────────────────────────

    @Operation(summary = "Initiate a refund for a successful payment")
    @PostMapping("/{id}/refund")
    public ResponseEntity<UnifiedPaymentOrderResponse> initiateRefund(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        PaymentOrder order = loadAndAssertAccess(id, principal);

        BigDecimal refundAmount = new BigDecimal(body.get("amount").toString());
        PaymentOrder updated = orderService.initiateRefund(order.getId(), refundAmount);
        return ResponseEntity.ok(UnifiedPaymentOrderResponse.from(updated));
    }

    // ─── Internal helpers ────────────────────────────────────────────────────────

    private PaymentOrder loadAndAssertAccess(Long id, UserPrincipal principal) {
        PaymentOrder order = orderRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Payment order not found: " + id));
        if (!principal.getCommunityId().equals(order.getCommunityId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Access denied to payment order " + id);
        }
        return order;
    }

    private void verifyCashfree(PaymentOrder order) {
        if (order.getGatewayOrderId() == null) return;
        var response = cashfreePaymentService.verifyPayment(order.getGatewayOrderId());
        if (response != null) {
            String pgStatus = response.getStatus() != null ? response.getStatus() : "";
            if ("PAID".equalsIgnoreCase(pgStatus) || "SUCCESS".equalsIgnoreCase(pgStatus)) {
                orderService.markSuccess(order.getIdempotencyKey(),
                    order.getGatewayOrderId(), order.getGatewayOrderId());
            } else if ("FAILED".equalsIgnoreCase(pgStatus) || "CANCELLED".equalsIgnoreCase(pgStatus)) {
                orderService.markFailed(order.getIdempotencyKey(), pgStatus);
            }
        }
    }

    private void verifyRazorpay(PaymentOrder order) {
        if (order.getGatewayOrderId() == null || order.getGatewayPaymentId() == null) return;
        VerifyPaymentRequest req = new VerifyPaymentRequest();
        req.setRazorpayOrderId(order.getGatewayOrderId());
        req.setRazorpayPaymentId(order.getGatewayPaymentId());
        var response = razorpayPaymentService.verifyPayment(req);
        if (response != null) {
            String pgStatus = response.getStatus() != null ? response.getStatus() : "";
            if ("PAID".equalsIgnoreCase(pgStatus) || "captured".equalsIgnoreCase(pgStatus)) {
                orderService.markSuccess(order.getIdempotencyKey(),
                    order.getGatewayPaymentId(), order.getGatewayOrderId());
            } else if ("FAILED".equalsIgnoreCase(pgStatus)) {
                orderService.markFailed(order.getIdempotencyKey(), pgStatus);
            }
        }
    }
}
