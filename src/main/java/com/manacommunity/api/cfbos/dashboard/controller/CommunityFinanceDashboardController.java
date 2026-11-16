package com.manacommunity.api.cfbos.dashboard.controller;

import com.manacommunity.api.cfbos.dashboard.dto.CommunityFinanceSummaryResponse;
import com.manacommunity.api.cfbos.dashboard.service.CommunityFinanceDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cfbos/dashboard")
@RequiredArgsConstructor
public class CommunityFinanceDashboardController {

    private final CommunityFinanceDashboardService dashboardService;

    /**
     * Returns the consolidated finance summary for a community.
     * Covers billing invoices, expenses, and payments scoped to the community.
     */
    @GetMapping("/summary")
    public ResponseEntity<CommunityFinanceSummaryResponse> getSummary(
            @RequestParam Long communityId) {
        return ResponseEntity.ok(dashboardService.getSummary(communityId));
    }
}
