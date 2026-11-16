package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.dto.*;
import com.manacommunity.api.hrm.entity.HrEmployee;
import com.manacommunity.api.hrm.service.HrAttendanceService;
import com.manacommunity.api.hrm.service.HrEmployeeService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hr/attendance")
@RequiredArgsConstructor
public class HrAttendanceController {

    private final HrAttendanceService attendanceService;
    private final HrEmployeeService employeeService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/check-in")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceResponse> checkIn(
            @RequestParam Long orgId,
            @Valid @RequestBody CheckInRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee employee = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(attendanceService.checkIn(employee, request));
    }

    @PostMapping("/check-out")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceResponse> checkOut(
            @RequestParam Long orgId,
            @Valid @RequestBody CheckOutRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee employee = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(attendanceService.checkOut(employee, request));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AttendanceResponse>> getMyAttendance(
            @RequestParam Long orgId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee employee = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(attendanceService.getMyAttendance(employee.getId(), from, to));
    }

    @GetMapping("/organizations/{orgId}/report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<AttendanceResponse>> getAttendanceReport(
            @PathVariable Long orgId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(attendanceService.getAttendanceReport(orgId, date, PageRequest.of(page, size)));
    }

    @PostMapping("/{attendanceId}/correct")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceResponse> correctAttendance(
            @PathVariable Long attendanceId,
            @RequestParam Long orgId,
            @Valid @RequestBody AttendanceCorrectionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(attendanceService.correctAttendance(orgId, attendanceId, request, user.getId()));
    }

    @PostMapping("/{attendanceId}/approve-overtime")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceResponse> approveOvertime(
            @PathVariable Long attendanceId,
            @RequestParam Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(attendanceService.approveOvertime(orgId, attendanceId, user.getId()));
    }

    @GetMapping("/organizations/{orgId}/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceSummaryResponse> getDailySummary(
            @PathVariable Long orgId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(attendanceService.getDailySummary(orgId, date));
    }

    // P0: manager-authorised employee-specific attendance
    @GetMapping("/organizations/{orgId}/employees/{employeeId}/records")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AttendanceResponse>> getEmployeeAttendanceRecords(
            @PathVariable Long orgId,
            @PathVariable Long employeeId,
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(attendanceService.getAttendanceByEmployee(orgId, employeeId, month));
    }

    // P1: break management
    @PostMapping("/break-start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceResponse> startBreak(
            @RequestParam Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee employee = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(attendanceService.startBreak(employee));
    }

    @PostMapping("/break-end")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceResponse> endBreak(
            @RequestParam Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee employee = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(attendanceService.endBreak(employee));
    }
}
