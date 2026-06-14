package com.manacommunity.api.controller;

import com.manacommunity.api.dto.dashboard.SportsDashboardResponse;
import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.LoggedInUserService;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.service.SportsDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.manacommunity.api.constants.PermissionConstants.VIEW_SPORTS_MAIN;

/**
 * Single aggregation endpoint backing the Sports Dashboard page. One call returns
 * exactly the data the page renders (stats, open/closed registration cards, the
 * user's upcoming events and registrations) instead of four full-entity fetches.
 */
@RestController
@RequestMapping("/api/sports")
@RequiredArgsConstructor
public class SportsDashboardController {

    private final SportsDashboardService dashboardService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping("/dashboard")
    public ResponseEntity<SportsDashboardResponse> getDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dashboardService.getDashboard(user));
    }
}
