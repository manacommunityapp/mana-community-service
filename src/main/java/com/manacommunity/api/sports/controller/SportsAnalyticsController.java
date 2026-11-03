package com.manacommunity.api.sports.controller;

import com.manacommunity.api.sports.dto.SportsAnalyticsResponse;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.sports.service.SportsAnalyticsService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

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
            @RequestParam(required = false) Long sportId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_ANALYTICS);
        validateDateRange(fromDate, toDate);
        AppUser user = loggedInUserService.resolve(principal);
        SportsAnalyticsResponse response = sportId == null && fromDate == null && toDate == null
                ? analyticsService.getAnalytics(user, communityId)
                : analyticsService.getAnalytics(user, communityId, sportId, fromDate, toDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/overview")
    public ResponseEntity<SportsAnalyticsResponse.OverviewMetrics> getOverview(
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) Long sportId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_ANALYTICS);
        validateDateRange(fromDate, toDate);
        AppUser user = loggedInUserService.resolve(principal);
        SportsAnalyticsResponse response = sportId == null && fromDate == null && toDate == null
                ? analyticsService.getAnalytics(user, communityId)
                : analyticsService.getAnalytics(user, communityId, sportId, fromDate, toDate);
        return ResponseEntity.ok(response.overview());
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromDate must be on or before toDate");
        }
    }
}
