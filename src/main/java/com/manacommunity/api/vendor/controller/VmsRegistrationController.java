package com.manacommunity.api.vendor.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.vendor.dto.VendorRegistrationRequest;
import com.manacommunity.api.vendor.dto.VendorRegistrationResponse;
import com.manacommunity.api.vendor.service.VmsRegistrationService;
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
@RequestMapping("/api/vendor/registrations")
@RequiredArgsConstructor
public class VmsRegistrationController {

    private final VmsRegistrationService registrationService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('Manage Vendors')")
    public ResponseEntity<Page<VendorRegistrationResponse>> getRegistrations(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(registrationService.getRegistrations(user.getCommunity().getId(), status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Vendors')")
    public ResponseEntity<VendorRegistrationResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(registrationService.getById(id, user.getCommunity().getId()));
    }

    @PostMapping
    public ResponseEntity<VendorRegistrationResponse> submit(
            @Valid @RequestBody VendorRegistrationRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.submit(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('Manage Vendors')")
    public ResponseEntity<VendorRegistrationResponse> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(registrationService.approve(id, user.getCommunity().getId(), user));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('Manage Vendors')")
    public ResponseEntity<VendorRegistrationResponse> reject(
            @PathVariable Long id,
            @RequestParam String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(registrationService.reject(id, user.getCommunity().getId(), reason, user));
    }
}
