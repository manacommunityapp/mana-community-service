package com.manacommunity.api.payments.controller;

import com.manacommunity.api.payments.dto.CreateOrderRequest;
import com.manacommunity.api.payments.dto.PaymentOrderResponse;
import com.manacommunity.api.payments.dto.VerifyPaymentRequest;
import com.manacommunity.api.payments.service.RazorpayPaymentService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for all Razorpay payment operations.
 *
 * <p>Disabled entirely when {@code app.razorpay.enabled=false} (returns 404).
 * Endpoint layout:
 * <ul>
 *   <li>{@code POST /api/payments/razorpay/orders}   — create a Razorpay order</li>
 *   <li>{@code POST /api/payments/razorpay/verify}   — verify payment after checkout</li>
 *   <li>{@code POST /api/payments/razorpay/webhook}  — Razorpay server-to-server webhook</li>
 *   <li>{@code GET  /api/payments/razorpay/orders}   — member's payment history</li>
 *   <li>{@code GET  /api/payments/razorpay/orders/community} — all community orders (admin)</li>
 *   <li>{@code GET  /api/payments/razorpay/orders/{id}}      — single order by DB id</li>
 * </ul>
 */
@Slf4j
@Tag(name = "Payments", description = "Razorpay payment operations")
@RestController
@RequestMapping("/api/payments/razorpay")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.razorpay.enabled", havingValue = "true")
public class RazorpayPaymentController {

    private final RazorpayPaymentService paymentService;
    private final LoggedInUserService    loggedInUserService;

    /**
     * Step 1: Create a Razorpay order.
     *
     * <p>Returns the order data — including the publishable {@code keyId} — that
     * the frontend uses to open the Razorpay checkout modal. Any authenticated
     * user may call this.
     */
    @Operation(summary = "Create Razorpay payment order")
    @PostMapping("/orders")
    public ResponseEntity<PaymentOrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        PaymentOrderResponse response = paymentService.createOrder(req, user, user.getCommunity());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Step 2: Verify payment after the Razorpay checkout modal succeeds.
     *
     * <p>The frontend sends the three IDs returned by Razorpay's success handler.
     * The backend verifies the HMAC-SHA256 signature before marking the order PAID.
     * Never trust the frontend callback result alone — always verify server-side.
     */
    @Operation(summary = "Verify Razorpay payment signature")
    @PostMapping("/verify")
    public ResponseEntity<PaymentOrderResponse> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Auth required — prevents unauthenticated callers from probing the endpoint.
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(paymentService.verifyPayment(req));
    }

    /**
     * Razorpay server-to-server webhook receiver.
     *
     * <p>This endpoint is {@code permitAll} in SecurityConfig because Razorpay
     * cannot send a JWT. Authenticity is enforced inside the service via HMAC-SHA256
     * verification of {@code X-Razorpay-Signature} against the raw body.
     *
     * <p>Always returns HTTP 200 immediately so Razorpay does not retry. Business
     * processing happens synchronously inside the service transaction.
     */
    @Operation(summary = "Razorpay webhook receiver (unauthenticated — verified by HMAC-SHA256)")
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        if (signature == null || signature.isBlank()) {
            log.warn("Razorpay webhook arrived without X-Razorpay-Signature header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Missing X-Razorpay-Signature header"));
        }

        paymentService.processWebhook(payload, signature);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    /**
     * Current user's payment history within their community (paginated, newest first).
     */
    @Operation(summary = "Get my payment orders")
    @GetMapping("/orders")
    public ResponseEntity<Page<PaymentOrderResponse>> getMyOrders(
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
     * All community payment orders — intended for admin reporting (paginated).
     */
    @Operation(summary = "Get all community payment orders (admin)")
    @GetMapping("/orders/community")
    public ResponseEntity<Page<PaymentOrderResponse>> getCommunityOrders(
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
     * Look up a single order by its local DB ID.
     */
    @Operation(summary = "Get payment order by ID")
    @GetMapping("/orders/{id}")
    public ResponseEntity<PaymentOrderResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(paymentService.getById(id, user.getCommunity().getId()));
    }

    /**
     * All orders for a specific business entity (e.g. all payments for event #42).
     */
    @Operation(summary = "Get payment orders for a specific business entity")
    @GetMapping("/orders/by-reference")
    public ResponseEntity<Page<PaymentOrderResponse>> getByReference(
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
}
