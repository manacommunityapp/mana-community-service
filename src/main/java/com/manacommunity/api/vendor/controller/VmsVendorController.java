package com.manacommunity.api.vendor.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.vendor.dto.VendorRequest;
import com.manacommunity.api.vendor.dto.VendorResponse;
import com.manacommunity.api.vendor.service.VmsVendorManagementService;
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
@RequestMapping("/api/vendor/vendors")
@RequiredArgsConstructor
public class VmsVendorController {

    private final VmsVendorManagementService vendorService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public ResponseEntity<Page<VendorResponse>> getVendors(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(vendorService.searchVendors(communityId, search, pageable));
        }
        return ResponseEntity.ok(vendorService.getVendors(communityId, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public ResponseEntity<VendorResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vendorService.getVendorById(id, user.getCommunity().getId()));
    }

    @GetMapping("/my-profile")
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public ResponseEntity<VendorResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vendorService.getMyVendorProfile(user.getId(), user.getCommunity().getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Vendor')")
    public ResponseEntity<VendorResponse> create(
            @Valid @RequestBody VendorRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vendorService.createVendor(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Vendors')")
    public ResponseEntity<VendorResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody VendorRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vendorService.updateVendor(id, req, user.getCommunity().getId()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('Manage Vendors')")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        vendorService.updateVendorStatus(id, status, user.getCommunity().getId());
        return ResponseEntity.ok().build();
    }
}
