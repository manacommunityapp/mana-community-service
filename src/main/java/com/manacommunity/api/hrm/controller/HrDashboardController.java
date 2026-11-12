package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.dto.HrDashboardResponse;
import com.manacommunity.api.hrm.service.HrDashboardService;
import com.manacommunity.api.hrm.service.HrEmployeeService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hr/dashboard")
@RequiredArgsConstructor
public class HrDashboardController {

    private final HrDashboardService dashboardService;
    private final HrEmployeeService employeeService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/organizations/{orgId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrDashboardResponse> getDashboard(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(dashboardService.getDashboard(orgId));
    }
}
