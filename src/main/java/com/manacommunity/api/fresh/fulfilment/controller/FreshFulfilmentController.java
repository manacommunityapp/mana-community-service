package com.manacommunity.api.fresh.fulfilment.controller;

import com.manacommunity.api.fresh.fulfilment.dto.FulfilmentGroupDto;
import com.manacommunity.api.fresh.fulfilment.dto.UpdateFulfilmentGroupStatusRequest;
import com.manacommunity.api.fresh.fulfilment.dto.VerifyHandoverRequest;
import com.manacommunity.api.fresh.fulfilment.service.FreshFulfilmentGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fresh/fulfilment")
@RequiredArgsConstructor
public class FreshFulfilmentController {

    private final FreshFulfilmentGroupService fulfilmentGroupService;

    @PostMapping("/orders/{orderId}/split")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'VENDOR')")
    public ResponseEntity<List<FulfilmentGroupDto>> splitOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(fulfilmentGroupService.splitOrderIntoFulfilmentGroups(orderId));
    }

    @GetMapping("/orders/{orderId}/groups")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'VENDOR', 'RESIDENT')")
    public ResponseEntity<List<FulfilmentGroupDto>> getGroupsByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(fulfilmentGroupService.getFulfilmentGroupsByOrder(orderId));
    }

    @GetMapping("/groups/{groupId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'VENDOR', 'DELIVERY_PARTNER', 'RESIDENT')")
    public ResponseEntity<FulfilmentGroupDto> getGroupById(@PathVariable Long groupId) {
        return ResponseEntity.ok(fulfilmentGroupService.getFulfilmentGroupById(groupId));
    }

    @GetMapping("/vendors/{vendorId}/groups")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'VENDOR')")
    public ResponseEntity<List<FulfilmentGroupDto>> getGroupsByVendor(@PathVariable Long vendorId) {
        return ResponseEntity.ok(fulfilmentGroupService.getFulfilmentGroupsByVendor(vendorId));
    }

    @PostMapping("/groups/{groupId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'VENDOR')")
    public ResponseEntity<FulfilmentGroupDto> updateStatus(
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateFulfilmentGroupStatusRequest req) {
        return ResponseEntity.ok(fulfilmentGroupService.updateStatus(groupId, req));
    }

    @PostMapping("/groups/{groupId}/verify-handover")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'VENDOR', 'DELIVERY_PARTNER', 'RESIDENT')")
    public ResponseEntity<FulfilmentGroupDto> verifyHandover(
            @PathVariable Long groupId,
            @Valid @RequestBody VerifyHandoverRequest req) {
        return ResponseEntity.ok(fulfilmentGroupService.verifyHandover(groupId, req));
    }
}
