package com.manacommunity.api.homeservices.controller;

import com.manacommunity.api.homeservices.dto.*;
import com.manacommunity.api.homeservices.model.DomesticStaff;
import com.manacommunity.api.homeservices.service.*;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Bridges mobile calls at /api/home-services (no /v1/) to the same service layer
 * as HomeServicesController. Needed because mobile homeServicesService.ts uses a
 * mix of /v1/home-services/... and /home-services/... paths, with path mismatches
 * (e.g. job-board vs jobs, packages/mine vs packages, staff/attendance vs attendance).
 */
@RestController
@RequestMapping("/api/home-services")
@RequiredArgsConstructor
public class HomeServicesBridgeController {

    private final DomesticStaffService staffService;
    private final StaffAttendanceService attendanceService;
    private final ServicePackageService packageService;
    private final StaffJobPostService jobPostService;
    private final LoggedInUserService loggedInUserService;

    /** GET /api/home-services/staff — alias for /api/v1/home-services/staff */
    @GetMapping({"/staff", "/domestic-staff"})
    public ResponseEntity<List<DomesticStaffResponse>> getStaff(
            @RequestParam(required = false) DomesticStaff.StaffRole role,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(staffService.getStaff(communityId, role));
    }

    /** GET /api/home-services/staff/attendance — mobile path; service has /api/v1/home-services/attendance */
    @GetMapping("/staff/attendance")
    public ResponseEntity<List<StaffAttendanceResponse>> getStaffAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(attendanceService.getAttendance(communityId,
                date != null ? date : LocalDate.now()));
    }

    /** GET /api/home-services/packages/mine — mobile path; service has /api/v1/home-services/packages */
    @GetMapping("/packages/mine")
    public ResponseEntity<List<ServicePackageResponse>> getMyPackages(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(packageService.getPackages(user));
    }

    /** GET /api/home-services/job-board — mobile alias for /api/v1/home-services/jobs */
    @GetMapping("/job-board")
    public ResponseEntity<List<StaffJobPostResponse>> getJobBoard(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(jobPostService.getJobPosts(communityId));
    }

    /** POST /api/home-services/job-board — mobile alias for POST /api/v1/home-services/jobs */
    @PostMapping("/job-board")
    public ResponseEntity<StaffJobPostResponse> postJob(
            @Valid @RequestBody StaffJobPostRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(jobPostService.createJobPost(request, user));
    }
}
