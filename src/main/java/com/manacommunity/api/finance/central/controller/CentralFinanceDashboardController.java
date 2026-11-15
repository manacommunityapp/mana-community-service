package com.manacommunity.api.finance.central.controller;

import com.manacommunity.api.finance.central.dto.CentralFinanceSummaryResponse;
import com.manacommunity.api.finance.central.service.CentralFinanceDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/central")
@RequiredArgsConstructor
public class CentralFinanceDashboardController {

    private final CentralFinanceDashboardService centralDashboardService;

    /**
     * Returns a consolidated finance summary across all modules for a community:
     * CFBOS billing, sports, events, marketplace, resource booking, society expenses,
     * and finance module invoices.
     *
     * GET /api/finance/central/summary?communityId={id}
     */
    @GetMapping("/summary")
    public ResponseEntity<CentralFinanceSummaryResponse> getSummary(@RequestParam Long communityId) {
        return ResponseEntity.ok(centralDashboardService.getSummary(communityId));
    }
}
