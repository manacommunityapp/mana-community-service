package com.manacommunity.api.vendor.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.vendor.dto.WorkOrderRequest;
import com.manacommunity.api.vendor.dto.WorkOrderResponse;
import com.manacommunity.api.vendor.service.VmsWorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendor/work-orders")
@RequiredArgsConstructor
public class VmsWorkOrderController {

    private final VmsWorkOrderService workOrderService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('Manage Work Orders')")
    public ResponseEntity<Page<WorkOrderResponse>> getWorkOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(workOrderService.getCommunityWorkOrders(user.getCommunity().getId(), status, pageable));
    }

    @GetMapping("/vendor/{vendorId}")
    @PreAuthorize("hasAuthority('Manage Work Orders')")
    public ResponseEntity<Page<WorkOrderResponse>> getVendorWorkOrders(
            @PathVariable Long vendorId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(workOrderService.getVendorWorkOrders(vendorId, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Work Orders')")
    public ResponseEntity<WorkOrderResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(workOrderService.getById(id, user.getCommunity().getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Manage Work Orders')")
    public ResponseEntity<WorkOrderResponse> create(
            @Valid @RequestBody WorkOrderRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workOrderService.create(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('Manage Work Orders')")
    public ResponseEntity<WorkOrderResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(workOrderService.updateStatus(id, status, user.getCommunity().getId()));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('Manage Work Orders')")
    public ResponseEntity<WorkOrderResponse> assignVendor(
            @PathVariable Long id,
            @RequestParam Long vendorId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(workOrderService.assignVendor(id, vendorId, user.getCommunity().getId()));
    }
}
