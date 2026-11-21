package com.manacommunity.api.cfbos.dashboard.controller;

import com.manacommunity.api.cfbos.dashboard.dto.CommunityFinanceSummaryResponse;
import com.manacommunity.api.cfbos.dashboard.service.CommunityFinanceDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/cfbos/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('View Finance') or hasAuthority('Admin')")
public class CommunityFinanceDashboardController {

    private final CommunityFinanceDashboardService dashboardService;

    /**
     * Returns the consolidated finance summary for a community.
     * Covers billing invoices, expenses, and payments scoped to the community.
     */
    @GetMapping("/summary")
    public ResponseEntity<CommunityFinanceSummaryResponse> getSummary(@AuthenticationPrincipal UserPrincipal principal
) {
        Long communityId = principal.getCommunityId();
        return ResponseEntity.ok(dashboardService.getSummary(communityId));
    }
}
