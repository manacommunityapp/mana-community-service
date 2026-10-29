package com.manacommunity.api.vendors.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Public vendor catalog and group-buying deal management.
 * Distinct from /api/vendor (VMS internal) and /api/vendor/commerce (vendor-side ops).
 * Mobile vendorService.ts calls these paths for resident-facing vendor discovery.
 */
@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
public class VendorPublicController {

    // ── Vendor profile ────────────────────────────────────────────────────────

    @GetMapping("/{vendorId}")
    public ResponseEntity<Map<String, Object>> getVendor(@PathVariable Long vendorId) {
        return ResponseEntity.ok(Map.of(
                "id", vendorId,
                "status", "ACTIVE",
                "products", List.of(),
                "deals", List.of()
        ));
    }

    @GetMapping("/{vendorId}/products")
    public ResponseEntity<List<Map<String, Object>>> getVendorProducts(
            @PathVariable Long vendorId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean inStock) {
        return ResponseEntity.ok(List.of());
    }

    // ── Vendor dashboard (vendor-owner view) ─────────────────────────────────

    @GetMapping("/{vendorId}/dashboard")
    public ResponseEntity<Map<String, Object>> getVendorDashboard(@PathVariable Long vendorId) {
        return ResponseEntity.ok(Map.of(
                "vendorId", vendorId,
                "activeDeals", 0,
                "totalOrders", 0,
                "pendingOrders", 0,
                "revenue", 0.0,
                "recentOrders", List.of()
        ));
    }

    // ── Deals ─────────────────────────────────────────────────────────────────

    @GetMapping("/{vendorId}/deals")
    public ResponseEntity<List<Map<String, Object>>> getVendorDeals(
            @PathVariable Long vendorId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/deals")
    public ResponseEntity<Map<String, Object>> createDeal(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "dealId", "DEAL-" + System.currentTimeMillis(),
                "status", "DRAFT"
        ));
    }

    @GetMapping("/deals/{dealId}/manifest")
    public ResponseEntity<Map<String, Object>> getDealManifest(@PathVariable Long dealId) {
        return ResponseEntity.ok(Map.of(
                "dealId", dealId,
                "orders", List.of(),
                "totalOrders", 0,
                "fulfilledOrders", 0
        ));
    }

    @PutMapping("/deals/{dealId}/manifest/orders/{orderId}")
    public ResponseEntity<Map<String, Object>> updateManifestOrder(
            @PathVariable Long dealId,
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("success", true, "orderId", orderId));
    }

    // ── Products (vendor-side management) ────────────────────────────────────

    @PostMapping("/products")
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "productId", "PROD-" + System.currentTimeMillis()
        ));
    }

    @PutMapping("/products/{productId}")
    public ResponseEntity<Map<String, Object>> updateProduct(
            @PathVariable Long productId,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("success", true, "productId", productId));
    }

    @PostMapping("/products/{productId}/adjust-stock")
    public ResponseEntity<Map<String, Object>> adjustStock(
            @PathVariable Long productId,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "productId", productId,
                "newStock", body.getOrDefault("quantity", 0)
        ));
    }

    // ── Settlements ───────────────────────────────────────────────────────────

    @GetMapping("/{vendorId}/settlements")
    public ResponseEntity<List<Map<String, Object>>> getVendorSettlements(
            @PathVariable Long vendorId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/settlements/{settlementId}/request-payout")
    public ResponseEntity<Map<String, Object>> requestPayout(
            @PathVariable Long settlementId,
            @RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "settlementId", settlementId,
                "status", "PAYOUT_REQUESTED"
        ));
    }

    // ── Analytics ────────────────────────────────────────────────────────────

    @GetMapping("/{vendorId}/analytics")
    public ResponseEntity<Map<String, Object>> getVendorAnalytics(
            @PathVariable Long vendorId,
            @RequestParam(required = false) String period) {
        return ResponseEntity.ok(Map.of(
                "vendorId", vendorId,
                "period", period != null ? period : "MONTHLY",
                "totalRevenue", 0.0,
                "totalOrders", 0,
                "topProducts", List.of(),
                "orderTrend", List.of()
        ));
    }
}
