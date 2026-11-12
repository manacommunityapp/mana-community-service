package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.entity.HrEmployee;
import com.manacommunity.api.hrm.entity.HrTimesheet;
import com.manacommunity.api.hrm.service.HrEmployeeService;
import com.manacommunity.api.hrm.service.HrTimesheetService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
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
@RequestMapping("/api/v1/hr/timesheets")
@RequiredArgsConstructor
public class HrTimesheetController {

    private final HrTimesheetService timesheetService;
    private final HrEmployeeService employeeService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/generate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrTimesheet> generateTimesheet(
            @RequestParam Long orgId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekDate,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee employee = employeeService.findByOrgAndManaUser(orgId, user.getId());
        LocalDate week = weekDate != null ? weekDate : LocalDate.now();
        return ResponseEntity.ok(timesheetService.generateWeeklyTimesheet(orgId, employee.getId(), week));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrTimesheet>> getMyTimesheets(
            @RequestParam Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        HrEmployee employee = employeeService.findByOrgAndManaUser(orgId, user.getId());
        return ResponseEntity.ok(timesheetService.getMyTimesheets(employee.getId()));
    }

    @PostMapping("/{timesheetId}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrTimesheet> submitTimesheet(
            @PathVariable Long timesheetId,
            @RequestParam Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(timesheetService.submitTimesheet(timesheetId));
    }

    @PostMapping("/{timesheetId}/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrTimesheet> approveTimesheet(
            @PathVariable Long timesheetId,
            @RequestParam Long orgId,
            @RequestParam(required = false) String remarks,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(timesheetService.approveTimesheet(timesheetId, user.getId(), remarks));
    }

    @GetMapping("/organizations/{orgId}/pending")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<HrTimesheet>> getPendingTimesheets(
            @PathVariable Long orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(timesheetService.getPendingTimesheets(orgId, PageRequest.of(page, size)));
    }
}
