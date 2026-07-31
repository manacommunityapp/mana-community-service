package com.manacommunity.api.vendor.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.vendor.dto.VendorDashboardResponse;
import com.manacommunity.api.vendor.dto.VendorPerformanceResponse;
import com.manacommunity.api.vendor.service.VmsDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendor/dashboard")
@RequiredArgsConstructor
public class VmsDashboardController {

    private final VmsDashboardService dashboardService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public ResponseEntity<VendorDashboardResponse> getDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dashboardService.getAdminDashboard(user.getCommunity().getId()));
    }

    @GetMapping("/performance")
    @PreAuthorize("hasAuthority('View Vendor Analytics')")
    public ResponseEntity<Page<VendorPerformanceResponse>> getPerformanceLeaderboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dashboardService.getPerformanceLeaderboard(user.getCommunity().getId(), PageRequest.of(page, size)));
    }
}
