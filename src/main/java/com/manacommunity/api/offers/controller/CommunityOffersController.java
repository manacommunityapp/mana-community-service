package com.manacommunity.api.offers.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Community Commerce Offers — hyperlocal deals, demand pools, farmers market days,
 * coupons, and vendor settlements for community residents.
 */
@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class CommunityOffersController {

    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, Object>>> getCategories(
            @RequestParam(required = false) Long communityId) {
        return ResponseEntity.ok(List.of(
                Map.of("id", "FOOD", "label", "Food & Grocery"),
                Map.of("id", "SERVICES", "label", "Services"),
                Map.of("id", "RETAIL", "label", "Retail"),
                Map.of("id", "FITNESS", "label", "Fitness & Wellness"),
                Map.of("id", "EDUCATION", "label", "Education")
        ));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getOffers(
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/{offerId}/claim")
    public ResponseEntity<Map<String, Object>> claimOffer(@PathVariable Long offerId) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "claimId", "CLM-" + offerId + "-" + System.currentTimeMillis(),
                "status", "CLAIMED"
        ));
    }

    @GetMapping("/my-claims")
    public ResponseEntity<List<Map<String, Object>>> myClaims(
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/claims/{claimId}/redeem")
    public ResponseEntity<Map<String, Object>> redeemClaim(
            @PathVariable Long claimId,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("success", true, "redeemedAt", java.time.LocalDateTime.now().toString()));
    }

    // ── Demand pools ──────────────────────────────────────────────────────────

    @GetMapping("/demand")
    public ResponseEntity<List<Map<String, Object>>> getDemandPools(
            @RequestParam(required = false) Long communityId) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/demand")
    public ResponseEntity<Map<String, Object>> createDemandPool(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("success", true, "id", "POOL-" + System.currentTimeMillis()));
    }

    @PostMapping("/demand/{poolId}/join")
    public ResponseEntity<Map<String, Object>> joinDemandPool(@PathVariable Long poolId) {
        return ResponseEntity.ok(Map.of("success", true, "poolId", poolId));
    }

    // ── Businesses ────────────────────────────────────────────────────────────

    @GetMapping("/businesses")
    public ResponseEntity<List<Map<String, Object>>> getBusinesses(
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/businesses/{id}")
    public ResponseEntity<Map<String, Object>> getBusiness(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("id", id, "status", "ACTIVE"));
    }

    // ── Farmers market days ───────────────────────────────────────────────────

    @GetMapping("/market-days")
    public ResponseEntity<List<Map<String, Object>>> getMarketDays(
            @RequestParam(required = false) Long communityId) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/market-days/{marketId}/rsvp")
    public ResponseEntity<Map<String, Object>> rsvpMarket(@PathVariable Long marketId) {
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/market-days/{marketId}/vendor-application")
    public ResponseEntity<Map<String, Object>> applyAsVendor(
            @PathVariable Long marketId,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("success", true, "applicationId", "APP-" + System.currentTimeMillis()));
    }

    // ── Coupons ───────────────────────────────────────────────────────────────

    @GetMapping("/coupons")
    public ResponseEntity<List<Map<String, Object>>> getCoupons(
            @RequestParam(required = false) Long communityId) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/coupons/validate")
    public ResponseEntity<Map<String, Object>> validateCoupon(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("valid", false, "message", "Coupon not found"));
    }

    // ── QR verification ───────────────────────────────────────────────────────

    @PostMapping("/deals/qr/verify")
    public ResponseEntity<Map<String, Object>> verifyQr(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("verified", false, "message", "QR code not found"));
    }

    // ── Settlements ───────────────────────────────────────────────────────────

    @GetMapping("/settlements")
    public ResponseEntity<List<Map<String, Object>>> getSettlements(
            @RequestParam(required = false) Long vendorId) {
        return ResponseEntity.ok(List.of());
    }
}
