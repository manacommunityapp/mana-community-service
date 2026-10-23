package com.manacommunity.api.vendor.commerce.controller;

import com.manacommunity.api.constants.PermissionConstants;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.vendor.commerce.dto.VmsVendorCommerceProfileRequest;
import com.manacommunity.api.vendor.commerce.dto.VmsVendorCommerceProfileResponse;
import com.manacommunity.api.vendor.commerce.service.VmsVendorCommerceProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/commerce/vendors")
@RequiredArgsConstructor
public class VmsVendorCommerceProfileController {

    private final VmsVendorCommerceProfileService profileService;
    private final LoggedInUserService loggedInUserService;

    /** Admin: list all commerce vendor profiles for a community */
    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.VIEW_VENDOR_MANAGEMENT + "')")
    public ResponseEntity<Page<VmsVendorCommerceProfileResponse>> listProfiles(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(profileService.listByCommunity(user.getCommunity().getId(), status, pageable));
    }

    /** Admin: get commerce profile for a specific vendor */
    @GetMapping("/{vendorId}/profile")
    @PreAuthorize("hasAuthority('" + PermissionConstants.VIEW_VENDOR_MANAGEMENT + "')")
    public ResponseEntity<VmsVendorCommerceProfileResponse> getProfile(
            @PathVariable Long vendorId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(profileService.getProfile(vendorId, user.getCommunity().getId()));
    }

    /** Vendor Portal: get own commerce profile */
    @GetMapping("/my-profile")
    @PreAuthorize("hasAuthority('" + PermissionConstants.VIEW_VENDOR_MANAGEMENT + "')")
    public ResponseEntity<VmsVendorCommerceProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(profileService.getMyProfile(user.getId(), user.getCommunity().getId()));
    }

    /** Vendor Portal: save / update own commerce profile */
    @PutMapping("/{vendorId}/profile")
    @PreAuthorize("hasAuthority('" + PermissionConstants.VIEW_VENDOR_MANAGEMENT + "')")
    public ResponseEntity<VmsVendorCommerceProfileResponse> upsertProfile(
            @PathVariable Long vendorId,
            @Valid @RequestBody VmsVendorCommerceProfileRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(profileService.upsertProfile(vendorId, user.getCommunity().getId(), request));
    }

    /** Vendor Portal: submit commerce profile for admin verification */
    @PostMapping("/{vendorId}/profile/submit")
    @PreAuthorize("hasAuthority('" + PermissionConstants.VIEW_VENDOR_MANAGEMENT + "')")
    public ResponseEntity<VmsVendorCommerceProfileResponse> submitForVerification(
            @PathVariable Long vendorId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(profileService.submitForVerification(vendorId, user.getCommunity().getId()));
    }

    /** Admin: approve / verify a commerce vendor profile */
    @PostMapping("/{vendorId}/profile/verify")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MANAGE_VENDORS + "')")
    public ResponseEntity<VmsVendorCommerceProfileResponse> verifyProfile(
            @PathVariable Long vendorId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser admin = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(profileService.verifyProfile(vendorId, admin.getCommunity().getId(), admin));
    }

    /** Admin: reject a commerce vendor profile */
    @PostMapping("/{vendorId}/profile/reject")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MANAGE_VENDORS + "')")
    public ResponseEntity<VmsVendorCommerceProfileResponse> rejectProfile(
            @PathVariable Long vendorId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser admin = loggedInUserService.resolve(principal);
        String reason = body.getOrDefault("reason", "");
        return ResponseEntity.ok(profileService.rejectProfile(vendorId, admin.getCommunity().getId(), reason, admin));
    }

    /** Admin: suspend a commerce vendor profile */
    @PostMapping("/{vendorId}/profile/suspend")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MANAGE_VENDORS + "')")
    public ResponseEntity<VmsVendorCommerceProfileResponse> suspendProfile(
            @PathVariable Long vendorId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser admin = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(profileService.suspendProfile(vendorId, admin.getCommunity().getId()));
    }
}
