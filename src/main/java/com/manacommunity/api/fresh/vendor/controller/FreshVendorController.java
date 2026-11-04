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
    public ResponseEntity<VendorCapabilityDto> applyForCapability(
            @Valid @RequestBody ApplyCapabilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(capabilityService.applyForCapability(request));
    }

    @GetMapping("/capabilities")
    public ResponseEntity<List<VendorCapabilityDto>> getVendorCapabilities(
            @RequestParam Long vendorId) {
        return ResponseEntity.ok(capabilityService.getCapabilitiesByVendor(vendorId));
    }

    @PostMapping("/staff")
    public ResponseEntity<VendorStaffDto> assignStaff(
            @Valid @RequestBody InviteStaffRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long invitingUserId = principal != null ? principal.getId() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.assignOrUpdateStaff(request, invitingUserId));
    }

    @GetMapping("/staff")
    public ResponseEntity<List<VendorStaffDto>> getVendorStaff(
            @RequestParam Long vendorId) {
        return ResponseEntity.ok(staffService.getStaffByVendor(vendorId));
    }

    @DeleteMapping("/staff/{staffUserId}")
    public ResponseEntity<Void> deactivateStaff(
            @RequestParam Long vendorId,
            @PathVariable Long staffUserId) {
        staffService.deactivateStaff(vendorId, staffUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/documents")
    public ResponseEntity<VendorDocumentDto> uploadDocument(
            @Valid @RequestBody UploadVendorDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.uploadDocument(request));
    }

    @GetMapping("/documents")
    public ResponseEntity<List<VendorDocumentDto>> getVendorDocuments(
            @RequestParam Long vendorId) {
        return ResponseEntity.ok(documentService.getVendorDocuments(vendorId));
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<FreshVendorDashboardSummaryDto> getDashboardSummary(
            @RequestParam Long vendorId) {
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

