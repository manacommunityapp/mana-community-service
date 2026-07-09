package com.manacommunity.api.jobs.controller;

import com.manacommunity.api.jobs.dto.ApplicationRequest;
import com.manacommunity.api.jobs.dto.JobRequest;
import com.manacommunity.api.jobs.dto.JobResponse;
import com.manacommunity.api.jobs.service.JobService;
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
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Jobs')")
    public ResponseEntity<List<JobResponse>> getActiveJobs(
            @RequestParam(required = false) String q,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(jobService.getActiveJobs(communityId, q, user.getId()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('View Jobs')")
    public ResponseEntity<List<JobResponse>> getAllJobs(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(jobService.getAllJobs(communityId, user.getId()));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('View Jobs')")
    public ResponseEntity<List<JobResponse>> getMyJobs(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jobService.getMyJobs(user.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Jobs')")
    public ResponseEntity<JobResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jobService.getById(id, user.getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Job')")
    public ResponseEntity<JobResponse> create(
            @Valid @RequestBody JobRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobService.create(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Job')")
    public ResponseEntity<JobResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody JobRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jobService.update(id, req, user.getId()));
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAuthority('Create Job')")
    public ResponseEntity<JobResponse> close(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jobService.closeJob(id, user.getId()));
    }

    @PostMapping("/{id}/apply")
    @PreAuthorize("hasAuthority('Apply Job')")
    public ResponseEntity<JobResponse> apply(
            @PathVariable Long id,
            @RequestBody(required = false) ApplicationRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jobService.apply(id, req, user));
    }
}
