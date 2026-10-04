package com.manacommunity.api.vendor.commerce.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.vendor.commerce.dto.*;
import com.manacommunity.api.vendor.commerce.service.VendorCommerceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendor/commerce")
@RequiredArgsConstructor
public class VendorCommerceController {

    private final VendorCommerceService commerceService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/products")
    public ResponseEntity<List<VendorProductDto>> getProducts(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.getVendorProducts(user.getId()));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<VendorProductDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(commerceService.getProductById(id));
    }

    @PostMapping("/products")
    public ResponseEntity<VendorProductDto> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.createProduct(user, request));
    }

    @PostMapping("/inventory/reserve")
    public ResponseEntity<InventoryReservationResponse> reserveInventory(
            @Valid @RequestBody InventoryReservationRequest request) {
        return ResponseEntity.ok(commerceService.reserveInventory(request));
    }

    @GetMapping("/stats")
    public ResponseEntity<VendorCommerceStatsDto> getStats(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commerceService.getStats(user.getId()));
    }
}
