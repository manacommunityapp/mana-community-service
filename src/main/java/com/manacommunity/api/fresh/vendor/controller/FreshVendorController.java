package com.manacommunity.api.fresh.vendor.controller;

import com.manacommunity.api.fresh.vendor.dto.*;
import com.manacommunity.api.fresh.vendor.service.FreshVendorCapabilityService;
import com.manacommunity.api.fresh.vendor.service.FreshVendorStaffService;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fresh/vendor")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
public class FreshVendorController {

    private final FreshVendorCapabilityService capabilityService;
    private final FreshVendorStaffService staffService;
    private final com.manacommunity.api.fresh.vendor.service.FreshVendorDocumentService documentService;

    @PostMapping("/capabilities/apply")
    @PreAuthorize("hasRole('ADMIN') or @freshSecurity.hasVendorRole(#principal, #request.vendorId, 'VENDOR_OWNER', 'VENDOR_MANAGER')")
    public ResponseEntity<VendorCapabilityDto> applyForCapability(
            @Valid @RequestBody ApplyCapabilityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(capabilityService.applyForCapability(request));
    }

    @GetMapping("/capabilities")
    @PreAuthorize("hasRole('ADMIN') or @freshSecurity.isVendorStaff(#principal, #vendorId)")
    public ResponseEntity<List<VendorCapabilityDto>> getVendorCapabilities(
            @RequestParam Long vendorId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(capabilityService.getCapabilitiesByVendor(vendorId));
    }

    @PostMapping("/staff")
    @PreAuthorize("hasRole('ADMIN') or @freshSecurity.hasVendorRole(#principal, #request.vendorId, 'VENDOR_OWNER', 'VENDOR_MANAGER')")
    public ResponseEntity<VendorStaffDto> assignStaff(
            @Valid @RequestBody InviteStaffRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long invitingUserId = principal != null ? principal.getId() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.assignOrUpdateStaff(request, invitingUserId));
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('ADMIN') or @freshSecurity.isVendorStaff(#principal, #vendorId)")
    public ResponseEntity<List<VendorStaffDto>> getVendorStaff(
            @RequestParam Long vendorId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(staffService.getStaffByVendor(vendorId));
    }

    @DeleteMapping("/staff/{staffUserId}")
    @PreAuthorize("hasRole('ADMIN') or @freshSecurity.hasVendorRole(#principal, #vendorId, 'VENDOR_OWNER')")
    public ResponseEntity<Void> deactivateStaff(
            @RequestParam Long vendorId,
            @PathVariable Long staffUserId,
            @AuthenticationPrincipal UserPrincipal principal) {
        staffService.deactivateStaff(vendorId, staffUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/documents")
    @PreAuthorize("hasRole('ADMIN') or @freshSecurity.hasVendorRole(#principal, #request.vendorId, 'VENDOR_OWNER', 'VENDOR_MANAGER', 'QUALITY_MANAGER')")
    public ResponseEntity<VendorDocumentDto> uploadDocument(
            @Valid @RequestBody UploadVendorDocumentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.uploadDocument(request));
    }

    @GetMapping("/documents")
    @PreAuthorize("hasRole('ADMIN') or @freshSecurity.isVendorStaff(#principal, #vendorId)")
    public ResponseEntity<List<VendorDocumentDto>> getVendorDocuments(
            @RequestParam Long vendorId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.getVendorDocuments(vendorId));
    }

    @GetMapping("/dashboard/summary")
    @PreAuthorize("hasRole('ADMIN') or @freshSecurity.isVendorStaff(#principal, #vendorId)")
    public ResponseEntity<FreshVendorDashboardSummaryDto> getDashboardSummary(
            @RequestParam Long vendorId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<VendorCapabilityDto> capabilities = capabilityService.getCapabilitiesByVendor(vendorId);

        FreshVendorDashboardSummaryDto summary = FreshVendorDashboardSummaryDto.builder()
                .vendorId(vendorId)
                .approvedCapabilities(capabilities)
                .todayOrdersCount(0)
                .tomorrowDemandUnits(0)
                .activeBatchesCount(0)
                .qualityPendingCount(0)
                .dispatchPendingCount(0)
                .monthToDateSales(BigDecimal.ZERO)
                .settlementDueAmount(BigDecimal.ZERO)
                .activeAlerts(new ArrayList<>())
                .build();

        return ResponseEntity.ok(summary);
    }
}

