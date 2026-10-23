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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/home-services")
@RequiredArgsConstructor
public class HomeServicesController {

    private final DomesticStaffService staffService;
    private final StaffAttendanceService attendanceService;
    private final ServicePackageService packageService;
    private final StaffJobPostService jobPostService;
    private final LoggedInUserService loggedInUserService;

    // ── Staff ───────────────────────────────────────────────────────

    @GetMapping({"/staff", "/domestic-staff"})
    public ResponseEntity<List<DomesticStaffResponse>> getStaff(
            @RequestParam(required = false) DomesticStaff.StaffRole role,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(staffService.getStaff(communityId, role));
    }

    @GetMapping("/staff/{id}")
    public ResponseEntity<DomesticStaffResponse> getStaffById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(staffService.getStaffById(id));
    }

    @PostMapping("/staff")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<DomesticStaffResponse> createStaff(
            @Valid @RequestBody DomesticStaffRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.createStaff(communityId, request));
    }

    // ── Attendance ──────────────────────────────────────────────────

    @GetMapping("/attendance")
    public ResponseEntity<List<StaffAttendanceResponse>> getAttendance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(attendanceService.getAttendance(communityId, date));
    }

    @GetMapping("/attendance/summary")
    public ResponseEntity<List<AttendanceSummaryResponse>> getAttendanceSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(attendanceService.getAttendanceSummary(communityId, from, to));
    }

    @PostMapping("/attendance")
    public ResponseEntity<StaffAttendanceResponse> markAttendance(
            @Valid @RequestBody StaffAttendanceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.markAttendance(request, user));
    }

    // ── Service Packages ────────────────────────────────────────────

    @GetMapping("/packages")
    public ResponseEntity<List<ServicePackageResponse>> getPackages(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(packageService.getPackages(user));
    }

    @PostMapping("/packages")
    public ResponseEntity<ServicePackageResponse> createPackage(
            @Valid @RequestBody ServicePackageRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(packageService.createPackage(request, user));
    }

    @PutMapping("/packages/{id}/mark-paid")
    public ResponseEntity<ServicePackageResponse> markPaid(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(packageService.markPaid(id));
    }

    // ── Job Posts ────────────────────────────────────────────────────

    @GetMapping("/jobs")
    public ResponseEntity<List<StaffJobPostResponse>> getJobPosts(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(jobPostService.getJobPosts(communityId));
    }

    @PostMapping("/jobs")
    public ResponseEntity<StaffJobPostResponse> createJobPost(
            @Valid @RequestBody StaffJobPostRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(jobPostService.createJobPost(request, user));
    }

    @PutMapping("/jobs/{id}/close")
    public ResponseEntity<StaffJobPostResponse> closeJobPost(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jobPostService.closeJobPost(id));
    }
}
