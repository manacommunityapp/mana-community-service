package com.manacommunity.api.vendor.commerce.controller;

import com.manacommunity.api.constants.PermissionConstants;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.vendor.commerce.dto.CreateProductRequest;
import com.manacommunity.api.vendor.commerce.dto.VendorProductDto;
import com.manacommunity.api.vendor.commerce.service.VendorCommerceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Vendor-facing product catalog endpoints consumed by the React frontend via
 * vendorProductService.ts (base path /api/v1/commerce/vendor).
 */
@RestController
@RequestMapping("/api/v1/commerce/vendor")
@RequiredArgsConstructor
public class VendorProductCatalogController {

    private final VendorCommerceService vendorCommerceService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/products")
    @PreAuthorize("hasAuthority('" + PermissionConstants.VIEW_COMMERCE_PRODUCTS + "')")
    public ResponseEntity<List<VendorProductDto>> getMyProducts(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vendorCommerceService.getVendorProducts(user.getId()));
    }

    @GetMapping("/products/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.VIEW_COMMERCE_PRODUCTS + "')")
    public ResponseEntity<VendorProductDto> getProductById(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vendorCommerceService.getProductById(id, user.getId()));
    }

    @PostMapping("/products")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MANAGE_COMMERCE_PRODUCTS + "')")
    public ResponseEntity<VendorProductDto> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vendorCommerceService.createProduct(user, request));
    }

    @PatchMapping("/products/{id}/deactivate")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MANAGE_COMMERCE_PRODUCTS + "')")
    public ResponseEntity<Void> deactivateProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        vendorCommerceService.deactivateProduct(user, id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/products/{id}/activate")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MANAGE_COMMERCE_PRODUCTS + "')")
    public ResponseEntity<Void> activateProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        vendorCommerceService.activateProduct(user, id);
        return ResponseEntity.ok().build();
    }
}
