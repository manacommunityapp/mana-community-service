package com.manacommunity.api.analytics.controller;

import com.manacommunity.api.analytics.model.CommunityInsight;
import com.manacommunity.api.analytics.model.CommunityMetricsSnapshot;
import com.manacommunity.api.analytics.service.CommunityInsightsService;
import com.manacommunity.api.platform.web.ApiResponse;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/insights")
public class CommunityInsightsController {

    private final CommunityInsightsService insightsService;

    public CommunityInsightsController(CommunityInsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CommunityInsight>>> getActiveInsights(
            @RequestParam Long communityId) {
        return ResponseEntity.ok(ApiResponse.ok(insightsService.getActiveInsights(communityId)));
    }

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<List<CommunityMetricsSnapshot>>> getMetricsTrend(
            @RequestParam Long communityId,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResponse.ok(insightsService.getMetricsTrend(communityId, days)));
    }

    @PostMapping("/snapshot")
    @PreAuthorize("hasAnyRole('FM','ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<ApiResponse<CommunityMetricsSnapshot>> triggerSnapshot(
            @RequestParam Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(insightsService.snapshotMetrics(communityId), "Snapshot taken"));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('FM','ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<ApiResponse<List<CommunityInsight>>> generateInsights(
            @RequestParam Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<CommunityInsight> insights = insightsService.generateInsights(communityId);
        return ResponseEntity.ok(ApiResponse.ok(insights, insights.size() + " insight(s) generated"));
    }

    @PutMapping("/{id}/dismiss")
    public ResponseEntity<ApiResponse<Map<String, String>>> dismissInsight(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        insightsService.dismissInsight(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "dismissed")));
    }
}
