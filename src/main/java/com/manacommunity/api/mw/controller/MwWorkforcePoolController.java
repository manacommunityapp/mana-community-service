package com.manacommunity.api.mw.controller;

import com.manacommunity.api.common.IdempotencyService;
import com.manacommunity.api.hrm.service.HrEmployeeService;
import com.manacommunity.api.mw.dto.*;
import com.manacommunity.api.mw.entity.*;
import com.manacommunity.api.mw.service.MwWorkforcePoolService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mw/pool")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MwWorkforcePoolController {

    private final MwWorkforcePoolService poolService;
    private final LoggedInUserService loggedInUserService;
    private final HrEmployeeService employeeService;
    private final IdempotencyService idempotencyService;

    // ── Employer: listings ────────────────────────────────────────────────────

    @PostMapping("/orgs/{orgId}/listings")
    public ResponseEntity<MwPoolListing> createListing(
            @PathVariable Long orgId,
            @Valid @RequestBody CreatePoolListingRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.verifyManagerAccess(orgId, user.getId());
        return ResponseEntity.ok(poolService.createListing(orgId, user.getId(), req));
    }

    @GetMapping("/orgs/{orgId}/listings")
    public ResponseEntity<List<MwPoolListing>> getOrgListings(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(poolService.getOrgListings(orgId));
    }

    @PatchMapping("/orgs/{orgId}/listings/{listingId}/close")
    public ResponseEntity<MwPoolListing> closeListing(
            @PathVariable Long orgId,
            @PathVariable Long listingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.verifyManagerAccess(orgId, user.getId());
        return ResponseEntity.ok(poolService.closeListing(orgId, listingId));
    }

    @GetMapping("/orgs/{orgId}/listings/{listingId}/applications")
    public ResponseEntity<List<MwPoolApplication>> getListingApplications(
            @PathVariable Long orgId,
            @PathVariable Long listingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(poolService.getListingApplications(orgId, listingId));
    }

    @PostMapping("/orgs/{orgId}/applications/{applicationId}/respond")
    public ResponseEntity<MwPoolApplication> respondToApplication(
            @PathVariable Long orgId,
            @PathVariable Long applicationId,
            @Valid @RequestBody RespondToApplicationRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.verifyManagerAccess(orgId, user.getId());
        return ResponseEntity.ok(poolService.respondToApplication(orgId, applicationId, req));
    }

    @PostMapping("/orgs/{orgId}/contracts/{contractId}/complete")
    public ResponseEntity<MwEngagementContract> completeEngagement(
            @PathVariable Long orgId,
            @PathVariable Long contractId,
            @Valid @RequestBody CompleteEngagementRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.verifyManagerAccess(orgId, user.getId());
        return ResponseEntity.ok(poolService.completeEngagement(orgId, contractId, req));
    }

    // ── Worker: browse & apply ────────────────────────────────────────────────

    @GetMapping("/browse")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> browseListings(
            @RequestParam(required = false) Long myOrgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(poolService.browseOpenListings(myOrgId).stream()
                .map(MwPoolListingPublicResponse::from)
                .toList());
    }

    @GetMapping("/listings/{listingId}")
    public ResponseEntity<MwPoolListing> getListing(
            @PathVariable Long listingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(poolService.getListing(listingId));
    }

    @PostMapping("/listings/{listingId}/apply")
    public ResponseEntity<MwPoolApplication> applyToListing(
            @PathVariable Long listingId,
            @RequestBody ApplyToListingRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(poolService.applyToListing(listingId, user.getId(), req));
    }

    @GetMapping("/my-applications")
    public ResponseEntity<List<MwPoolApplication>> getMyApplications(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(poolService.getMyApplications(user.getId()));
    }

    @PostMapping("/contracts/{contractId}/consent")
    public ResponseEntity<MwEngagementContract> consentToContract(
            @PathVariable Long contractId,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal) {
        idempotencyService.checkAndRegister(idempotencyKey);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(poolService.consentToContract(user.getId(), contractId));
    }

    @GetMapping("/applications/{applicationId}/contract")
    public ResponseEntity<MwEngagementContract> getContractByApplication(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(poolService.getContractByApplication(user.getId(), applicationId));
    }
}
