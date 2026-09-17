package com.manacommunity.api.controller;

import com.manacommunity.api.dto.dashboard.SportsAnalyticsResponse;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.service.SportsAnalyticsService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.manacommunity.api.constants.permissions.SportsPermissions.VIEW_SPORTS_ANALYTICS;

/**
 * Controller exposing comprehensive sports analytics endpoints.
 * Restricted strictly to Admin and Sports Admin roles via VIEW_SPORTS_ANALYTICS permission.
 */
@RestController
@RequestMapping("/api/sports/analytics")
@RequiredArgsConstructor
public class SportsAnalyticsController {

    private final SportsAnalyticsService analyticsService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping
    public ResponseEntity<SportsAnalyticsResponse> getAnalytics(
            @RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_ANALYTICS);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(analyticsService.getAnalytics(user, communityId));
    }

    @GetMapping("/overview")
    public ResponseEntity<SportsAnalyticsResponse.OverviewMetrics> getOverview(
            @RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_ANALYTICS);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(analyticsService.getAnalytics(user, communityId).overview());
    }
}
