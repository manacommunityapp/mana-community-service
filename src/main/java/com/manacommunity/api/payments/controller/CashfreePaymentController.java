package com.manacommunity.api.payments.controller;

import com.manacommunity.api.payments.dto.CashfreeAdminSummaryResponse;
import com.manacommunity.api.payments.dto.CashfreeCreateOrderRequest;
import com.manacommunity.api.payments.dto.CashfreeOrderResponse;
import com.manacommunity.api.payments.dto.CashfreeRefundRequest;
import com.manacommunity.api.payments.service.CashfreePaymentService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.Map;

/**
 * REST controller for all Cashfree PG payment operations.
 *
 * <p>Disabled entirely when {@code app.cashfree.enabled=false}.
 * Endpoint layout:
 * <ul>
 *   <li>{@code POST /api/payments/cashfree/orders}                     — create a Cashfree order</li>
 *   <li>{@code POST /api/payments/cashfree/orders/{orderId}/verify}    — verify payment after checkout</li>
 *   <li>{@code POST /api/payments/cashfree/webhook}                    — Cashfree server-to-server webhook</li>
 *   <li>{@code GET  /api/payments/cashfree/orders}                     — member's payment history</li>
 *   <li>{@code GET  /api/payments/cashfree/orders/community}           — all community orders (admin)</li>
 *   <li>{@code GET  /api/payments/cashfree/orders/{id}}                — single order by DB id</li>
 *   <li>{@code GET  /api/payments/cashfree/orders/by-reference}        — orders for a business entity</li>
 *   <li>{@code GET  /api/payments/cashfree/admin/report}               — admin filtered transaction report (ADMIN only)</li>
 *   <li>{@code GET  /api/payments/cashfree/admin/summary}              — admin aggregate stats (ADMIN only)</li>
 *   <li>{@code POST /api/payments/cashfree/admin/refund/{orderId}}     — admin-initiated refund (ADMIN only)</li>
 * </ul>
 */
@Slf4j
@Tag(name = "Payments", description = "Cashfree PG payment operations")
@RestController
@RequestMapping("/api/payments/cashfree")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.cashfree.enabled", havingValue = "true")
public class CashfreePaymentController {

    private final CashfreePaymentService paymentService;
    private final LoggedInUserService    loggedInUserService;

    /**
     * Step 1: Create a Cashfree order.
     *
     * <p>Returns the order data including {@code paymentSessionId} and {@code appId}
     * that the mobile/web SDK uses to open the checkout sheet.
     */
    @Operation(summary = "Create Cashfree payment order")
    @PostMapping("/orders")
    public ResponseEntity<CashfreeOrderResponse> createOrder(
            @Valid @RequestBody CashfreeCreateOrderRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        CashfreeOrderResponse response =
                paymentService.createOrder(req, user, user.getCommunity());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Step 2: Verify payment after the SDK checkout completes.
     *
     * <p>The SDK's {@code onVerify} callback provides the order ID.
     * The backend re-queries Cashfree to confirm status — the frontend
     * callback result alone is not trusted.
     */
    @Operation(summary = "Verify Cashfree payment status")
    @PostMapping("/orders/{orderId}/verify")
    public ResponseEntity<CashfreeOrderResponse> verifyPayment(
            @PathVariable String orderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(paymentService.verifyPayment(orderId));
    }

    /**
     * Cashfree server-to-server webhook receiver.
     *
     * <p>This endpoint is {@code permitAll} in SecurityConfig because Cashfree
     * cannot send a JWT. Authenticity is enforced inside the service via
     * HMAC-SHA256 verification of {@code x-webhook-signature} against
     * (x-webhook-timestamp + rawBody).
     *
     * <p>Always returns HTTP 200 so Cashfree does not retry.
     */
    @Operation(summary = "Cashfree webhook receiver (unauthenticated — verified by HMAC-SHA256)")
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "x-webhook-signature",  required = false) String signature,
            @RequestHeader(value = "x-webhook-timestamp",  required = false) String timestamp) {

        if (signature == null || signature.isBlank()) {
            log.warn("Cashfree webhook arrived without x-webhook-signature header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Missing x-webhook-signature header"));
        }
        if (timestamp == null || timestamp.isBlank()) {
            log.warn("Cashfree webhook arrived without x-webhook-timestamp header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Missing x-webhook-timestamp header"));
        }

        paymentService.processWebhook(payload, signature, timestamp);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    /**
     * Current user's Cashfree payment history within their community (paginated).
     */
    @Operation(summary = "Get my Cashfree payment orders")
    @GetMapping("/orders")
    public ResponseEntity<Page<CashfreeOrderResponse>> getMyOrders(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(
                paymentService.getMyOrders(user.getId(), user.getCommunity().getId(), pageable));
    }

    /**
     * All community Cashfree orders — for admin reporting (paginated).
     */
    @Operation(summary = "Get all community Cashfree orders (admin)")
    @GetMapping("/orders/community")
    public ResponseEntity<Page<CashfreeOrderResponse>> getCommunityOrders(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(
                paymentService.getCommunityOrders(user.getCommunity().getId(), pageable));
    }

    /**
     * Look up a single Cashfree order by its local DB ID.
     */
    @Operation(summary = "Get Cashfree payment order by ID")
    @GetMapping("/orders/{id}")
    public ResponseEntity<CashfreeOrderResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(paymentService.getById(id, user.getCommunity().getId()));
    }

    /**
     * Initiate a refund for a PAID order.
     *
     * <p>Transitions the order to REFUND_PENDING. The final REFUNDED state
     * arrives via the REFUND_STATUS_WEBHOOK from Cashfree.
     */
    @Operation(summary = "Initiate a refund for a Cashfree order")
    @PostMapping("/orders/{orderId}/refund")
    public ResponseEntity<CashfreeOrderResponse> initiateRefund(
            @PathVariable String orderId,
            @Valid @RequestBody CashfreeRefundRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(paymentService.initiateRefund(orderId, req.getAmount(), req.getReason()));
    }

    /**
     * All orders for a specific business entity.
     */
    @Operation(summary = "Get Cashfree orders for a business entity")
    @GetMapping("/orders/by-reference")
    public ResponseEntity<Page<CashfreeOrderResponse>> getByReference(
            @RequestParam String referenceType,
            @RequestParam Long   referenceId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(
                paymentService.getByReference(
                        referenceType, referenceId, user.getCommunity().getId(), pageable));
    }

    // ─── Admin Endpoints ──────────────────────────────────────────────────────

    /**
     * Filterable paginated transaction report for admins.
     *
     * <p>Requires ADMIN or SUPER_ADMIN role (enforced in SecurityConfig via
     * {@code /api/payments/cashfree/admin/**}).
     *
     * @param status    optional status filter (e.g. PAID, FAILED)
     * @param refType   optional referenceType filter (e.g. SPORTS_REGISTRATION)
     * @param from      optional ISO date start, e.g. 2026-01-01
     * @param to        optional ISO date end, e.g. 2026-12-31
     */
    @Operation(summary = "Admin: filtered payment transaction report")
    @GetMapping("/admin/report")
    public ResponseEntity<Page<CashfreeOrderResponse>> getAdminReport(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String refType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt   = to   != null ? to.atTime(23, 59, 59) : null;
        return ResponseEntity.ok(
                paymentService.getAdminReport(
                        user.getCommunity().getId(), status, refType, fromDt, toDt, pageable));
    }

    /**
     * Aggregate payment statistics for the admin dashboard.
     *
     * <p>Requires ADMIN or SUPER_ADMIN role.
     */
    @Operation(summary = "Admin: aggregate payment summary statistics")
    @GetMapping("/admin/summary")
    public ResponseEntity<CashfreeAdminSummaryResponse> getAdminSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt   = to   != null ? to.atTime(23, 59, 59) : null;
        return ResponseEntity.ok(
                paymentService.getAdminSummary(user.getCommunity().getId(), fromDt, toDt));
    }

    /**
     * Admin-initiated refund: allows an admin to refund any PAID order
     * in their community without being the original payer.
     *
     * <p>Requires ADMIN or SUPER_ADMIN role.
     */
    @Operation(summary = "Admin: initiate refund for any community order")
    @PostMapping("/admin/refund/{orderId}")
    public ResponseEntity<CashfreeOrderResponse> adminInitiateRefund(
            @PathVariable String orderId,
            @Valid @RequestBody CashfreeRefundRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(paymentService.initiateRefund(orderId, req.getAmount(), req.getReason()));
    }
}
