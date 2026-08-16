package com.manacommunity.api.cfbos.tax.controller;

import com.manacommunity.api.cfbos.tax.dto.*;
import com.manacommunity.api.cfbos.tax.engine.TaxEngine;
import com.manacommunity.api.cfbos.tax.service.TaxConfigService;
import com.manacommunity.api.constants.permissions.AdminPermissions;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.service.PermissionCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/cfbos/v1/tax")
@RequiredArgsConstructor
public class TaxController {

    private final TaxEngine taxEngine;
    private final TaxConfigService taxConfigService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping("/gst/config")
    public ResponseEntity<TaxConfigDto> getGstConfig(@AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, AdminPermissions.VIEW_ADMIN);
        return ResponseEntity.ok(taxConfigService.getTaxConfig());
    }

    @PutMapping("/gst/config")
    public ResponseEntity<TaxConfigDto> updateGstConfig(@AuthenticationPrincipal UserPrincipal principal,
                                                         @RequestBody TaxConfigDto dto) {
        permissionCheckService.requireAnyPermission(principal, AdminPermissions.VIEW_ADMIN);
        return ResponseEntity.ok(taxConfigService.updateTaxConfig(dto));
    }

    @GetMapping("/gst/rates")
    public ResponseEntity<List<TaxRateDto>> getTaxRates(@AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, AdminPermissions.VIEW_ADMIN);
        return ResponseEntity.ok(taxConfigService.getAllTaxRates());
    }

    @PostMapping("/gst/calculate")
    public ResponseEntity<GstCalculationResult> calculateGst(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam BigDecimal taxableAmount,
            @RequestParam BigDecimal cgstRate,
            @RequestParam BigDecimal sgstRate) {
        permissionCheckService.requireAnyPermission(principal, AdminPermissions.VIEW_ADMIN);
        return ResponseEntity.ok(taxEngine.calculateGst(taxableAmount, cgstRate, sgstRate));
    }

    @PostMapping("/tds/calculate")
    public ResponseEntity<TdsCalculationResult> calculateTds(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam BigDecimal grossAmount,
            @RequestParam String tdsSection,
            @RequestParam String payeeType) {
        permissionCheckService.requireAnyPermission(principal, AdminPermissions.VIEW_ADMIN);
        return ResponseEntity.ok(taxEngine.calculateTds(grossAmount, tdsSection, payeeType));
    }

    @GetMapping("/hsn-sac")
    public ResponseEntity<List<HsnSacCodeDto>> getHsnSacCodes(@AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, AdminPermissions.VIEW_ADMIN);
        return ResponseEntity.ok(taxConfigService.getAllHsnSacCodes());
    }

    @PostMapping("/hsn-sac")
    public ResponseEntity<HsnSacCodeDto> createHsnSacCode(@AuthenticationPrincipal UserPrincipal principal,
                                                           @RequestBody HsnSacCodeDto dto) {
        permissionCheckService.requireAnyPermission(principal, AdminPermissions.VIEW_ADMIN);
        return ResponseEntity.ok(taxConfigService.createHsnSacCode(dto));
    }

    @GetMapping("/tds/sections")
    public ResponseEntity<List<TdsSectionDto>> getTdsSections(@AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, AdminPermissions.VIEW_ADMIN);
        return ResponseEntity.ok(taxConfigService.getAllTdsSections());
    }
}
