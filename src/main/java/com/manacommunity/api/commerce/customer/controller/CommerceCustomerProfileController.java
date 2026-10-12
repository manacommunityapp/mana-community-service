package com.manacommunity.api.commerce.customer.controller;

import com.manacommunity.api.commerce.customer.dto.*;
import com.manacommunity.api.commerce.customer.service.CommerceCustomerProfileService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/commerce/customer")
@RequiredArgsConstructor
public class CommerceCustomerProfileController {

    private final CommerceCustomerProfileService profileService;
    private final LoggedInUserService loggedInUserService;

    /** GET  /api/v1/commerce/customer/me  — get or auto-create the caller's profile */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommerceCustomerProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(profileService.getOrCreateMyProfile(user));
    }

    /** PUT  /api/v1/commerce/customer/me  — update display name / phone / preferences */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommerceCustomerProfileResponse> updateMyProfile(
            @Valid @RequestBody CommerceCustomerProfileUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(profileService.updateMyProfile(user, request));
    }

    /** GET  /api/v1/commerce/customer/me/addresses */
    @GetMapping("/me/addresses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CommerceCustomerAddressResponse>> getMyAddresses(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        CommerceCustomerProfileResponse profile = profileService.getOrCreateMyProfile(user);
        return ResponseEntity.ok(profile.getAddresses());
    }

    /** POST /api/v1/commerce/customer/me/addresses */
    @PostMapping("/me/addresses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommerceCustomerAddressResponse> addAddress(
            @Valid @RequestBody CommerceCustomerAddressRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(profileService.addAddress(user, request));
    }

    /** PUT  /api/v1/commerce/customer/me/addresses/{id} */
    @PutMapping("/me/addresses/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommerceCustomerAddressResponse> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody CommerceCustomerAddressRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(profileService.updateAddress(user, id, request));
    }

    /** DELETE /api/v1/commerce/customer/me/addresses/{id} */
    @DeleteMapping("/me/addresses/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        profileService.deleteAddress(user, id);
        return ResponseEntity.noContent().build();
    }
}
