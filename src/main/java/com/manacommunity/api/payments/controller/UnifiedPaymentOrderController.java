package com.manacommunity.api.payments.controller;

import com.manacommunity.api.payments.config.dto.RecordManualPaymentRequest;
import com.manacommunity.api.payments.dto.CreatePaymentOrderRequest;
import com.manacommunity.api.payments.dto.InitiateManaPayRequest;
import com.manacommunity.api.payments.dto.InitiateRefundRequest;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
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
    private final ObjectProvider<CashfreePaymentService> cashfreePaymentService;
    private final ObjectProvider<RazorpayPaymentService> razorpayPaymentService;

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

    // ─── ManaPay (wallet) ────────────────────────────────────────────────────────

    /**
     * Initiates a ManaPay wallet payment for an order previously created with
     * gateway=MANA_PAY.  Wallet debit is synchronous — the response will be
     * SUCCESS or FAILED immediately (no PG redirect).
     *
     * <p>Financial rule: amount comes from the stored order — never from this request.
     */
    @Operation(summary = "Pay via ManaPay wallet (synchronous internal debit)")
    @PostMapping("/mana-pay/initiate")
    public ResponseEntity<UnifiedPaymentOrderResponse> initiateManaPayPayment(
            @Valid @RequestBody InitiateManaPayRequest body,
            @AuthenticationPrincipal UserPrincipal principal) {

        PaymentOrder order = loadAndAssertAccess(body.getOrderId(), principal);

        // Community isolation already asserted by loadAndAssertAccess
        PaymentOrder updated = orderService.initiateManaPayOrder(order.getId());
        return ResponseEntity.ok(UnifiedPaymentOrderResponse.from(updated));
    }

    // ─── Refund ──────────────────────────────────────────────────────────────────

    @Operation(summary = "Initiate a refund for a successful payment")
    @PostMapping("/{id}/refund")
    public ResponseEntity<UnifiedPaymentOrderResponse> initiateRefund(
            @PathVariable Long id,
            @Valid @RequestBody InitiateRefundRequest body,
            @AuthenticationPrincipal UserPrincipal principal) {

        PaymentOrder order = loadAndAssertAccess(id, principal);

        PaymentOrder updated = orderService.initiateRefund(order.getId(), body.getAmount());
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
        // CashfreePaymentService.verifyPayment() fetches live status from Cashfree, updates
        // the CashfreeOrder record, and calls dispatchBusinessConfirmation() which fires
        // GatewayPaymentCapturedEvent / markSuccess() internally.  The controller must NOT
        // call markSuccess() a second time — just trigger the service and let the state
        // machine transitions happen through the canonical path.
        cashfreePaymentService.ifAvailable(service -> service.verifyPayment(order.getGatewayOrderId()));
    }

    private void verifyRazorpay(PaymentOrder order) {
        if (order.getGatewayOrderId() == null || order.getGatewayPaymentId() == null) return;
        razorpayPaymentService.ifAvailable(service -> {
            VerifyPaymentRequest req = new VerifyPaymentRequest();
            req.setRazorpayOrderId(order.getGatewayOrderId());
            req.setRazorpayPaymentId(order.getGatewayPaymentId());
            var response = service.verifyPayment(req);
            if (response != null) {
                String pgStatus = response.getStatus() != null ? response.getStatus() : "";
                if ("PAID".equalsIgnoreCase(pgStatus) || "captured".equalsIgnoreCase(pgStatus)) {
                    orderService.markSuccess(order.getIdempotencyKey(),
                        order.getGatewayPaymentId(), order.getGatewayOrderId());
                } else if ("FAILED".equalsIgnoreCase(pgStatus)) {
                    orderService.markFailed(order.getIdempotencyKey(), pgStatus);
                }
            }
        });
    }

    // ─── Manual payment ──────────────────────────────────────────────────────────

    /**
     * Records an offline payment (cash/cheque/UPI) made directly to the community.
     * Only community admins, finance admins, and cashiers may call this.
     */
    @Operation(summary = "Record a manual (offline) payment for a community order")
    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN','FINANCE_MGMT_ADMIN','CASHIER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<UnifiedPaymentOrderResponse> recordManualPayment(
            @RequestBody RecordManualPaymentRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        // Community isolation
        Long principalCommunityId = principal.getCommunityId();
        if (principalCommunityId == null || !principalCommunityId.equals(req.getCommunityId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "communityId in request does not match authenticated user's community");
        }

        PaymentOrder recorded = orderService.recordManualPayment(req, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(UnifiedPaymentOrderResponse.from(recorded));
    }
}
