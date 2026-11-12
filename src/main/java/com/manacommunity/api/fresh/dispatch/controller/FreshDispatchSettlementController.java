package com.manacommunity.api.fresh.dispatch.controller;

import com.manacommunity.api.fresh.dispatch.dto.*;
import com.manacommunity.api.fresh.dispatch.model.DeliveryBatchStatus;
import com.manacommunity.api.fresh.dispatch.model.PurchaseOrderStatus;
import com.manacommunity.api.fresh.dispatch.model.SettlementStatus;
import com.manacommunity.api.fresh.dispatch.service.FreshDispatchSettlementService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fresh/dispatch")
@RequiredArgsConstructor
public class FreshDispatchSettlementController {

    private final FreshDispatchSettlementService dispatchService;

    // ----- Purchase Orders -----

    @PostMapping("/purchase-orders")
    @PreAuthorize("hasRole('ADMIN') or @freshSecurity.hasVendorRole(#principal, #req.vendorId, 'VENDOR_OWNER', 'VENDOR_MANAGER')")
    public ResponseEntity<PurchaseOrderDto> createPurchaseOrder(
            @RequestBody CreatePurchaseOrderRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(dispatchService.createPurchaseOrder(req));
    }

    @GetMapping("/purchase-orders")
    @PreAuthorize("hasRole('ADMIN') or @freshSecurity.isVendorStaff(#principal, #vendorId)")
    public ResponseEntity<List<PurchaseOrderDto>> getPurchaseOrders(
            @RequestParam Long vendorId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(dispatchService.getPurchaseOrdersByVendor(vendorId));
    }

    @PutMapping("/purchase-orders/{poId}/status")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PurchaseOrderDto> updatePurchaseOrderStatus(
            @PathVariable Long poId,
            @RequestParam PurchaseOrderStatus status) {
        return ResponseEntity.ok(dispatchService.updatePurchaseOrderStatus(poId, status));
    }

    // ----- Delivery Dispatch Batches -----

    @PostMapping("/delivery-batches")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<DeliveryBatchDto> createDeliveryBatch(@RequestBody CreateDeliveryBatchRequest req) {
        return ResponseEntity.ok(dispatchService.createDeliveryBatch(req));
    }

    @GetMapping("/delivery-batches")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<DeliveryBatchDto>> getDeliveryBatches(
            @RequestParam Long communityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(dispatchService.getDeliveryBatches(communityId, date));
    }

    @PutMapping("/delivery-batches/{batchId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<DeliveryBatchDto> updateDeliveryBatchStatus(
            @PathVariable Long batchId,
            @RequestParam DeliveryBatchStatus status) {
        return ResponseEntity.ok(dispatchService.updateDeliveryBatchStatus(batchId, status));
    }

    @PutMapping("/delivery-items/{itemId}/handover")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'VENDOR')")
    public ResponseEntity<DeliveryItemDto> updateHandover(
            @PathVariable Long itemId,
            @RequestBody UpdateHandoverRequest req) {
        return ResponseEntity.ok(dispatchService.updateHandover(itemId, req));
    }

    // ----- Settlements -----

    @PostMapping("/settlements")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<VendorSettlementDto> createSettlement(@RequestBody CreateSettlementRequest req) {
        return ResponseEntity.ok(dispatchService.createSettlement(req));
    }

    @GetMapping("/settlements")
    @PreAuthorize("hasRole('ADMIN') or (#vendorId != null and @freshSecurity.hasVendorRole(#principal, #vendorId, 'VENDOR_OWNER', 'VENDOR_MANAGER', 'VENDOR_ACCOUNTANT'))")
    public ResponseEntity<List<VendorSettlementDto>> getSettlements(
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) SettlementStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (vendorId != null) {
            return ResponseEntity.ok(dispatchService.getSettlementsByVendor(vendorId));
        }
        if (status != null) {
            return ResponseEntity.ok(dispatchService.getSettlementsByStatus(status));
        }
        return ResponseEntity.ok(dispatchService.getSettlementsByStatus(SettlementStatus.PENDING));
    }

    @PostMapping("/settlements/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<VendorSettlementDto> approveSettlement(@PathVariable Long id) {
        return ResponseEntity.ok(dispatchService.approveSettlement(id));
    }

    @PostMapping("/settlements/{id}/process")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<VendorSettlementDto> processSettlement(
            @PathVariable Long id,
            @RequestBody ProcessSettlementRequest req) {
        return ResponseEntity.ok(dispatchService.processSettlement(id, req));
    }
}