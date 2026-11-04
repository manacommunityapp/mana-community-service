package com.manacommunity.api.fresh.catalog.controller;

import com.manacommunity.api.fresh.catalog.dto.*;
import com.manacommunity.api.fresh.catalog.service.FreshCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fresh/catalog")
@RequiredArgsConstructor
public class FreshCatalogController {

    private final FreshCatalogService catalogService;

    // --- Full catalog read for a product ---
    @GetMapping("/products/{productId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<FreshProductCatalogDto> getCatalog(@PathVariable Long productId) {
        return ResponseEntity.ok(catalogService.getCatalog(productId));
    }

    // --- Catalog profile (shelf life, origin, certifications) ---
    @PostMapping("/products/profile")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<FreshProductCatalogDto> saveProfile(@RequestBody FreshCatalogProfileRequest request) {
        return ResponseEntity.ok(catalogService.saveOrUpdateProfile(request));
    }

    // --- Dynamic attributes ---
    @PutMapping("/products/{productId}/attributes")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<FreshProductAttributeDto>> saveAttributes(
            @PathVariable Long productId,
            @RequestBody FreshProductAttributeRequest request) {
        return ResponseEntity.ok(catalogService.saveAttributes(productId, request));
    }

    @GetMapping("/products/{productId}/attributes")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<FreshProductAttributeDto>> getAttributes(@PathVariable Long productId) {
        return ResponseEntity.ok(catalogService.getAttributes(productId));
    }

    // --- Nutrition ---
    @PutMapping("/products/nutrition")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<FreshProductNutritionDto> saveNutrition(@RequestBody FreshProductNutritionDto dto) {
        return ResponseEntity.ok(catalogService.saveOrUpdateNutrition(dto));
    }

    // --- Allergens ---
    @PutMapping("/products/{productId}/allergens")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<FreshProductAllergenDto>> saveAllergens(
            @PathVariable Long productId,
            @RequestBody List<FreshProductAllergenDto> dtos) {
        return ResponseEntity.ok(catalogService.saveAllergens(productId, dtos));
    }

    // --- Pricing Simulator ---
    @PostMapping("/products/pricing/simulate")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<FreshProductPricingDto> simulatePricing(@RequestBody PricingSimulatorRequest request) {
        return ResponseEntity.ok(catalogService.simulateAndSavePricing(request));
    }

    @GetMapping("/products/{productId}/pricing/active")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<FreshProductPricingDto> getActivePricing(@PathVariable Long productId) {
        return ResponseEntity.ok(catalogService.getActivePricing(productId));
    }

    // --- Admin: Pricing Approval ---
    @GetMapping("/admin/pricing/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<FreshProductPricingDto>> getPendingPricingApprovals() {
        return ResponseEntity.ok(catalogService.getPendingAdminApproval());
    }

    @PostMapping("/admin/pricing/{pricingId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<FreshProductPricingDto> approvePricing(@PathVariable Long pricingId) {
        return ResponseEntity.ok(catalogService.approvePricing(pricingId, null));
    }
}