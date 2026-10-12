package com.manacommunity.api.commerce.core.controller;

import com.manacommunity.api.commerce.core.dto.*;
import com.manacommunity.api.commerce.core.model.CommerceChannel;
import com.manacommunity.api.commerce.core.model.CommerceOrderStatus;
import com.manacommunity.api.commerce.core.model.CommerceProduct;
import com.manacommunity.api.commerce.core.model.CommerceRefund;
import com.manacommunity.api.commerce.core.dto.CommerceAnalyticsDto;
import com.manacommunity.api.commerce.core.service.CommerceAnalyticsService;
import com.manacommunity.api.commerce.core.service.CommerceCoreService;
import com.manacommunity.api.commerce.core.service.CommercePaymentService;
import com.manacommunity.api.finance.dto.FinanceDocumentDto;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/commerce")
@RequiredArgsConstructor
public class CommerceCoreController {

    private final CommerceCoreService commerceService;
    private final CommercePaymentService paymentService;
    private final CommerceAnalyticsService analyticsService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/products")
    public ResponseEntity<List<CommerceProduct>> getProducts(
            @RequestParam(required = false) CommerceChannel channel) {
        return ResponseEntity.ok(commerceService.getProducts(channel));
    }

    @PostMapping("/orders/checkout")
    public ResponseEntity<CommerceOrderDto> checkout(
            @Valid @RequestBody CommerceCheckoutRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.checkout(user, request));
    }

    @GetMapping("/orders/my")
    public ResponseEntity<List<CommerceOrderDto>> getMyOrders(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.getMyOrders(user));
    }

    @GetMapping("/orders/{orderNumber}")
    public ResponseEntity<CommerceOrderDto> getOrderByNumber(
            @PathVariable String orderNumber,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.getOrderByNumber(user, orderNumber));
    }

    @PostMapping("/handover/verify")
    public ResponseEntity<HandoverVerificationResponse> verifyHandover(
            @Valid @RequestBody HandoverVerificationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.verifyHandover(user, request));
    }

    @PostMapping("/reviews")
    public ResponseEntity<CommerceReviewDto> submitReview(
            @Valid @RequestBody CommerceReviewDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.submitReview(user, dto));
    }

    @PostMapping("/disputes")
    public ResponseEntity<CommerceDisputeDto> raiseDispute(
            @Valid @RequestBody CommerceDisputeDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.raiseDispute(user, dto));
    }

    @PostMapping("/orders/{orderNumber}/refund")
    public ResponseEntity<CommerceRefund> initiateRefund(
            @PathVariable String orderNumber,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String reason = body.getOrDefault("reason", "Customer requested cancellation");
        return ResponseEntity.ok(commerceService.processRefund(user, orderNumber, reason));
    }

    @GetMapping("/settlements/my")
    public ResponseEntity<List<CommerceSettlementDto>> getMySettlements(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.getMySettlements(user));
    }

    // ── Phase 4: order lifecycle transitions ──────────────────────────────────

    /** Buyer or admin cancels a PENDING/CONFIRMED/PROCESSING order. */
    @PostMapping("/orders/{orderNumber}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommerceOrderDto> cancelOrder(
            @PathVariable String orderNumber,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String reason = body != null ? body.getOrDefault("reason", "Cancelled by customer") : "Cancelled by customer";
        return ResponseEntity.ok(commerceService.cancelOrder(user, orderNumber, reason));
    }

    /** Vendor starts processing a CONFIRMED order. */
    @PostMapping("/orders/{orderNumber}/process")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommerceOrderDto> processOrder(
            @PathVariable String orderNumber,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String note = body != null ? body.get("vendorNote") : null;
        return ResponseEntity.ok(commerceService.processOrder(user, orderNumber, note));
    }

    /** Vendor marks order ready for pickup/dispatch. */
    @PostMapping("/orders/{orderNumber}/ready")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommerceOrderDto> markReady(
            @PathVariable String orderNumber,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.markReadyForPickup(user, orderNumber));
    }

    /** Vendor dispatches order for delivery. */
    @PostMapping("/orders/{orderNumber}/dispatch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommerceOrderDto> dispatchOrder(
            @PathVariable String orderNumber,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String eta = body != null ? body.get("estimatedDeliveryDate") : null;
        return ResponseEntity.ok(commerceService.markOutForDelivery(user, orderNumber, eta));
    }

    /** Vendor / delivery agent confirms delivery. */
    @PostMapping("/orders/{orderNumber}/delivered")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommerceOrderDto> markDelivered(
            @PathVariable String orderNumber,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.markDelivered(user, orderNumber));
    }

    /** Vendor portal: get orders assigned to the logged-in vendor. */
    @GetMapping("/orders/vendor")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CommerceOrderDto>> getVendorOrders(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        CommerceOrderStatus statusEnum = parseStatus(status);
        return ResponseEntity.ok(commerceService.getVendorOrders(user, statusEnum));
    }

    /** Admin: get all community orders paged, optionally filtered by status. */
    @GetMapping("/orders/admin")
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<Page<CommerceOrderDto>> getAdminOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        CommerceOrderStatus statusEnum = parseStatus(status);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(commerceService.getAdminOrders(user, statusEnum, pageable));
    }

    // ── Phase 7: Settlement & Payouts ─────────────────────────────────────────

    /** Admin runs a settlement cycle for a seller/vendor. */
    @PostMapping("/settlements/run")
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<CommerceSettlementDto> runSettlementCycle(
            @Valid @RequestBody CommerceSettlementRunRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.runSettlementCycle(user, request));
    }

    /** Admin marks a settlement as paid with a UTR reference. */
    @PostMapping("/settlements/{settlementNumber}/pay")
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<CommerceSettlementDto> markSettlementPaid(
            @PathVariable String settlementNumber,
            @Valid @RequestBody CommerceSettlementMarkPaidRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.markSettlementPaid(user, settlementNumber, request));
    }

    /** Admin views all settlement records for the community (paged). */
    @GetMapping("/settlements/admin")
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<Page<CommerceSettlementDto>> getAdminSettlements(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("cycleEndDate").descending());
        return ResponseEntity.ok(commerceService.getAdminSettlements(user, status, pageable));
    }

    // ── Phase 6: Invoice ──────────────────────────────────────────────────────

    /** Buyer fetches the auto-generated GST invoice for a COMPLETED order. */
    @GetMapping("/orders/{orderNumber}/invoice")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FinanceDocumentDto> getOrderInvoice(
            @PathVariable String orderNumber,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return commerceService.getOrderInvoice(user, orderNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    // ── Phase 5: Razorpay payment processing ─────────────────────────────────

    /** Buyer initiates payment — creates Razorpay order, returns keyId + razorpayOrderId. */
    @PostMapping("/orders/{orderNumber}/pay/initiate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommercePaymentInitiateResponse> initiatePayment(
            @PathVariable String orderNumber,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(paymentService.initiatePayment(user, orderNumber));
    }

    /** Buyer verifies Razorpay HMAC signature after checkout success. */
    @PostMapping("/orders/{orderNumber}/pay/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommerceOrderDto> verifyPayment(
            @PathVariable String orderNumber,
            @Valid @RequestBody CommercePaymentVerifyRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(paymentService.verifyAndCapture(user, orderNumber, request));
    }

    // ── Phase 11: Analytics ───────────────────────────────────────────────────

    @GetMapping("/analytics")
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<CommerceAnalyticsDto> getAnalytics(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(analyticsService.getAnalytics(user, from, to));
    }

    // ── Phase 10: Loyalty & Rewards ──────────────────────────────────────────

    @GetMapping("/loyalty/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommerceLoyaltyDto> getMyLoyalty(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.getMyLoyalty(user));
    }

    @PostMapping("/loyalty/redeem")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommerceLoyaltyDto> redeemLoyaltyPoints(
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        long points = body.getOrDefault("points", 0L);
        return ResponseEntity.ok(commerceService.redeemLoyaltyPoints(user, points));
    }

    // ── Phase 15: Dispute Resolution ─────────────────────────────────────────

    @GetMapping("/disputes/admin")
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<Page<CommerceDisputeDto>> getAdminDisputes(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.getAdminDisputes(user, status,
                org.springframework.data.domain.PageRequest.of(page, size)));
    }

    @PostMapping("/disputes/{disputeCode}/resolve")
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<CommerceDisputeDto> resolveDispute(
            @PathVariable String disputeCode,
            @RequestBody java.util.Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String resolution = body.getOrDefault("resolution", "ACKNOWLEDGED");
        String adminNote = body.getOrDefault("adminNote", "");
        return ResponseEntity.ok(commerceService.resolveDispute(user, disputeCode, resolution, adminNote));
    }

    // ── Phase 14: Vendor Ratings ──────────────────────────────────────────────

    @GetMapping("/ratings/vendors")
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<List<Map<String, Object>>> getVendorRatings(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.getVendorRatings(user));
    }

    @GetMapping("/ratings/{targetType}/{targetId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getVendorRating(
            @PathVariable String targetType,
            @PathVariable String targetId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.getVendorRating(user, targetType, targetId));
    }

    private CommerceOrderStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return CommerceOrderStatus.valueOf(raw.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}
