package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.service.HrAnalyticsService;
import com.manacommunity.api.hrm.service.HrEmployeeService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hr/analytics")
@RequiredArgsConstructor
public class HrAnalyticsController {

    private final HrAnalyticsService analyticsService;
    private final HrEmployeeService employeeService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/organizations/{orgId}/dashboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getDashboard(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(analyticsService.getHrDashboard(orgId));
    }

    @GetMapping("/organizations/{orgId}/attrition-trend")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getAttritionTrend(
            @PathVariable Long orgId,
            @RequestParam(defaultValue = "12") int months,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(analyticsService.getAttritionTrend(orgId, months));
    }

    @GetMapping("/organizations/{orgId}/headcount-trend")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getHeadcountTrend(
            @PathVariable Long orgId,
            @RequestParam(defaultValue = "12") int months,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(analyticsService.getHeadcountTrend(orgId, months));
    }

    @GetMapping("/organizations/{orgId}/department-breakdown")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getDepartmentBreakdown(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(analyticsService.getDepartmentBreakdown(orgId));
    }

    @GetMapping("/organizations/{orgId}/attendance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getAttendanceAnalytics(
            @PathVariable Long orgId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        int y = year != null ? year : LocalDate.now().getYear();
        int m = month != null ? month : LocalDate.now().getMonthValue();
        return ResponseEntity.ok(analyticsService.getAttendanceAnalytics(orgId, y, m));
    }

    @GetMapping("/organizations/{orgId}/leave")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getLeaveAnalytics(
            @PathVariable Long orgId,
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(analyticsService.getLeaveAnalytics(orgId, y));
    }

    @GetMapping("/organizations/{orgId}/workforce-metrics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getWorkforceMetrics(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(analyticsService.getWorkforceMetrics(orgId));
    }
}
