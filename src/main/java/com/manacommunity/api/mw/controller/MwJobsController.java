package com.manacommunity.api.mw.controller;

import com.manacommunity.api.mw.dto.*;
import com.manacommunity.api.mw.service.MwJobsService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mw/jobs")
@RequiredArgsConstructor
public class MwJobsController {

    private final MwJobsService jobsService;
    private final LoggedInUserService loggedInUserService;

    // ── Community job board (public) ─────────────────────────────────────────

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getCommunityJobs(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(jobsService.getCommunityJobs(search));
    }

    // ── Employer: manage org listings ────────────────────────────────────────

    @GetMapping("/orgs/{orgId}/listings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getOrgListings(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jobsService.getOrgListings(orgId));
    }

    @PostMapping("/orgs/{orgId}/listings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createListing(
            @PathVariable Long orgId,
            @Valid @RequestBody CreateJobListingRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jobsService.createJobListing(orgId, user.getId(), req));
    }

    @PatchMapping("/orgs/{orgId}/listings/{jobId}/pause")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> pauseListing(
            @PathVariable Long orgId,
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jobsService.pauseListing(orgId, jobId));
    }

    @PatchMapping("/orgs/{orgId}/listings/{jobId}/close")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> closeListing(
            @PathVariable Long orgId,
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jobsService.closeListing(orgId, jobId));
    }

    @GetMapping("/orgs/{orgId}/listings/{jobId}/applications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getApplications(
            @PathVariable Long orgId,
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jobsService.getJobApplications(orgId, jobId));
    }

    @PostMapping("/orgs/{orgId}/listings/{jobId}/applications/{appId}/respond")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> respondToApplication(
            @PathVariable Long orgId,
            @PathVariable Long jobId,
            @PathVariable Long appId,
            @Valid @RequestBody JobApplicationResponseRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jobsService.respondToApplication(orgId, jobId, appId, req));
    }

    // ── Worker: apply and track ───────────────────────────────────────────────

    @PostMapping("/listings/{jobId}/apply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> applyToJob(
            @PathVariable Long jobId,
            @RequestBody ApplyToJobRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jobsService.applyToJob(jobId, user.getId(), req));
    }

    @GetMapping("/my-applications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyApplications(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jobsService.getMyApplications(user.getId()));
    }
}
