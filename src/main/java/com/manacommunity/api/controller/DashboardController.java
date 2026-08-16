package com.manacommunity.api.controller;

import com.manacommunity.api.dto.dashboard.AdminDashboardStatsResponse;
import com.manacommunity.api.dto.dashboard.UserDashboardStatsResponse;
import com.manacommunity.api.service.DashboardService;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.manacommunity.api.constants.PermissionConstants.VIEW_ADMIN_DASHBOARD;
import static com.manacommunity.api.constants.PermissionConstants.VIEW_USER_DASHBOARD;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping("/admin/stats")
    public ResponseEntity<AdminDashboardStatsResponse> getAdminStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN_DASHBOARD);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dashboardService.getAdminStats(caller));
    }

    @GetMapping("/user/stats")
    public ResponseEntity<UserDashboardStatsResponse> getUserStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_USER_DASHBOARD);
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dashboardService.getUserStats(caller));
    }
}
