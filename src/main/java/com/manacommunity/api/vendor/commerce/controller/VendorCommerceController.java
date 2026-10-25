package com.manacommunity.api.vendor.commerce.controller;

import com.manacommunity.api.constants.PermissionConstants;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.vendor.commerce.dto.*;
import com.manacommunity.api.vendor.commerce.service.VendorCommerceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vendor/commerce")
@RequiredArgsConstructor
public class VendorCommerceController {

    private final VendorCommerceService commerceService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/products")
    @PreAuthorize("hasAuthority('" + PermissionConstants.VIEW_COMMERCE_PRODUCTS + "')")
    public ResponseEntity<List<VendorProductDto>> getProducts(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.getVendorProducts(user.getId()));
    }

    @GetMapping("/products/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.VIEW_COMMERCE_PRODUCTS + "')")
    public ResponseEntity<VendorProductDto> getProductById(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.getProductById(id, user.getId()));
    }

    @GetMapping("/grocery-stores")
    @PreAuthorize("hasAuthority('" + PermissionConstants.VIEW_COMMERCE_PRODUCTS + "')")
    public ResponseEntity<List<Map<String, Object>>> getGroceryStores(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.getVendorGroceryStores(user));
    }

    @GetMapping("/grocery-stores/{storeId}/categories")
    @PreAuthorize("hasAuthority('" + PermissionConstants.VIEW_COMMERCE_PRODUCTS + "')")
    public ResponseEntity<List<Map<String, Object>>> getGroceryCategories(
            @PathVariable Long storeId, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.getVendorGroceryCategories(user, storeId));
    }

    @PostMapping("/products")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MANAGE_COMMERCE_PRODUCTS + "')")
    public ResponseEntity<VendorProductDto> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.createProduct(user, request));
    }

    @PostMapping("/variants/{id}/adjust-stock")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MANAGE_COMMERCE_INVENTORY + "')")
    public ResponseEntity<Void> adjustStock(
            @PathVariable Long id,
            @Valid @RequestBody StockAdjustmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        commerceService.adjustStock(id, request, user.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/inventory/reserve")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MANAGE_COMMERCE_ORDERS + "')")
    public ResponseEntity<InventoryReservationResponse> reserveInventory(
            @Valid @RequestBody InventoryReservationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.reserveInventory(request, user.getId()));
    }

    @GetMapping("/inventory/batches")
    @PreAuthorize("hasAuthority('" + PermissionConstants.VIEW_COMMERCE_INVENTORY + "')")
    public ResponseEntity<List<VendorInventoryBatchDto>> getBatches(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.getBatches(user.getId()));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('" + PermissionConstants.VIEW_COMMERCE_INVENTORY + "')")
    public ResponseEntity<VendorCommerceStatsDto> getStats(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.getStats(user.getId()));
    }

    @GetMapping("/settlements")
    @PreAuthorize("hasAuthority('" + PermissionConstants.VIEW_COMMERCE_SETTLEMENTS + "')")
    public ResponseEntity<List<VendorSettlementDto>> getSettlements(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.getSettlements(user.getId()));
    }

    @PostMapping("/settlements/{id}/request-payout")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MANAGE_COMMERCE_SETTLEMENTS + "')")
    public ResponseEntity<Map<String, Object>> requestPayout(
            @PathVariable String id, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.requestPayout(id, user.getId()));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAuthority('" + PermissionConstants.VIEW_COMMERCE_ANALYTICS + "')")
    public ResponseEntity<VendorAnalyticsDto> getAnalytics(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.getAnalytics(user.getId()));
    }
}
