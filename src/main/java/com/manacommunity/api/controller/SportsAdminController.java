package com.manacommunity.api.controller;

import com.manacommunity.api.dto.dashboard.SportsAdminFormDataResponse;
import com.manacommunity.api.dto.dashboard.SportsAdminOverviewResponse;
import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.LoggedInUserService;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.service.SportsAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.manacommunity.api.constants.PermissionConstants.VIEW_SPORTS_MAIN;

/**
 * Lean aggregation endpoints for the Sports Admin page:
 *
 * <ul>
 *   <li>{@code GET /api/sports/admin/overview}  — tournaments + events lists (Dashboard / Sports Event tabs)</li>
 *   <li>{@code GET /api/sports/admin/form-data} — sports + categories + communities (Create forms)</li>
 * </ul>
 *
 * Each replaces several full-entity fetches with one trimmed response.
 */
@RestController
@RequestMapping("/api/sports/admin")
@RequiredArgsConstructor
public class SportsAdminController {

    private final SportsAdminService adminService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping("/overview")
    public ResponseEntity<SportsAdminOverviewResponse> overview(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(adminService.getOverview(user));
    }

    @GetMapping("/form-data")
    public ResponseEntity<SportsAdminFormDataResponse> formData(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(adminService.getFormData(user));
    }
}
