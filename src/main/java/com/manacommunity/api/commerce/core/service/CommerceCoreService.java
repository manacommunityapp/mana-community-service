package com.manacommunity.api.commerce.core.service;

import com.manacommunity.api.commerce.core.dto.*;
import com.manacommunity.api.commerce.core.model.CommerceChannel;
import com.manacommunity.api.commerce.core.model.CommerceOrderStatus;
import com.manacommunity.api.commerce.core.model.CommerceProduct;
import com.manacommunity.api.commerce.core.model.CommerceRefund;
import com.manacommunity.api.finance.dto.FinanceDocumentDto;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CommerceCoreService {

    List<CommerceProduct> getProducts(CommerceChannel channel);

    CommerceOrderDto checkout(AppUser buyer, CommerceCheckoutRequest request);

    List<CommerceOrderDto> getMyOrders(AppUser buyer);

    CommerceOrderDto getOrderByNumber(AppUser user, String orderNumber);

    CommerceOrderDto getOrderByNumber(String orderNumber);

    HandoverVerificationResponse verifyHandover(AppUser verifier, HandoverVerificationRequest request);

    CommerceReviewDto submitReview(AppUser reviewer, CommerceReviewDto reviewDto);

    CommerceDisputeDto raiseDispute(AppUser buyer, CommerceDisputeDto disputeDto);

    CommerceRefund processRefund(AppUser user, String orderNumber, String reason);

    List<CommerceSettlementDto> getMySettlements(AppUser seller);

    // ── Phase 4: order lifecycle transitions ──────────────────────────────────

    /** Buyer or admin cancels a PENDING / CONFIRMED order. */
    CommerceOrderDto cancelOrder(AppUser user, String orderNumber, String reason);

    /** Vendor acknowledges and starts processing a CONFIRMED order. */
    CommerceOrderDto processOrder(AppUser vendor, String orderNumber, String vendorNote);

    /** Vendor marks order as packed and ready for collection. */
    CommerceOrderDto markReadyForPickup(AppUser vendor, String orderNumber);

    /** Vendor dispatches order for home delivery. */
    CommerceOrderDto markOutForDelivery(AppUser vendor, String orderNumber, String estimatedDeliveryDate);

    /** Delivery agent or vendor marks order as delivered. */
    CommerceOrderDto markDelivered(AppUser user, String orderNumber);

    /** Vendor views their assigned orders, optionally filtered by status. */
    List<CommerceOrderDto> getVendorOrders(AppUser vendor, CommerceOrderStatus status);

    /** Admin sees all orders in the community (paged). */
    Page<CommerceOrderDto> getAdminOrders(AppUser admin, CommerceOrderStatus status, Pageable pageable);

    // ── Phase 6: Invoice ──────────────────────────────────────────────────────

    /** Returns the auto-generated GST invoice for a completed order, if available. */
    Optional<FinanceDocumentDto> getOrderInvoice(AppUser user, String orderNumber);

    // ── Phase 7: Settlement & Payouts ─────────────────────────────────────────

    /** Admin runs a settlement cycle for a seller/vendor, aggregating COMPLETED orders. */
    CommerceSettlementDto runSettlementCycle(AppUser admin, CommerceSettlementRunRequest request);

    /** Admin marks a PENDING settlement as PAID with a UTR reference. */
    CommerceSettlementDto markSettlementPaid(AppUser admin, String settlementNumber, CommerceSettlementMarkPaidRequest request);

    /** Admin views all settlements for the community (paged). */
    Page<CommerceSettlementDto> getAdminSettlements(AppUser admin, String status, Pageable pageable);

    // ── Phase 10: Loyalty & Rewards ──────────────────────────────────────────

    CommerceLoyaltyDto getMyLoyalty(AppUser user);

    CommerceLoyaltyDto redeemLoyaltyPoints(AppUser user, long points);

    // ── Phase 14: Vendor Ratings ──────────────────────────────────────────────

    List<Map<String, Object>> getVendorRatings(AppUser admin);

    Map<String, Object> getVendorRating(AppUser user, String targetType, String targetId);

    // ── Phase 15: Dispute Resolution ─────────────────────────────────────────

    Page<CommerceDisputeDto> getAdminDisputes(AppUser admin, String status, Pageable pageable);

    CommerceDisputeDto resolveDispute(AppUser admin, String disputeCode, String resolution, String adminNote);
}
