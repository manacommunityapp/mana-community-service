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

    @GetMapping({"", "/active"})
    public ResponseEntity<ApiResponse<List<CommunityInsight>>> getActiveInsights(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long communityId) {
        Long cid = (principal != null && principal.getCommunityId() != null)
                ? principal.getCommunityId() : (communityId != null ? communityId : 1L);
        return ResponseEntity.ok(ApiResponse.ok(insightsService.getActiveInsights(cid)));
    }

    @GetMapping({"/metrics", "/trend"})
    public ResponseEntity<ApiResponse<List<CommunityMetricsSnapshot>>> getMetricsTrend(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long communityId,
            @RequestParam(defaultValue = "30") int days) {
        Long cid = (principal != null && principal.getCommunityId() != null)
                ? principal.getCommunityId() : (communityId != null ? communityId : 1L);
        return ResponseEntity.ok(ApiResponse.ok(insightsService.getMetricsTrend(cid, days)));
    }

    @PostMapping("/snapshot")
    @PreAuthorize("hasAnyRole('FM','ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<ApiResponse<CommunityMetricsSnapshot>> triggerSnapshot(
            @RequestParam(required = false) Long communityId,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long cid = (principal != null && principal.getCommunityId() != null)
                ? principal.getCommunityId()
                : (communityId != null ? communityId : (body != null && body.containsKey("communityId") ? Long.valueOf(body.get("communityId").toString()) : 1L));
        return ResponseEntity.ok(ApiResponse.ok(insightsService.snapshotMetrics(cid), "Snapshot taken"));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('FM','ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<ApiResponse<List<CommunityInsight>>> generateInsights(
            @RequestParam(required = false) Long communityId,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long cid = (principal != null && principal.getCommunityId() != null)
                ? principal.getCommunityId()
                : (communityId != null ? communityId : (body != null && body.containsKey("communityId") ? Long.valueOf(body.get("communityId").toString()) : 1L));
        List<CommunityInsight> insights = insightsService.generateInsights(cid);
        return ResponseEntity.ok(ApiResponse.ok(insights, insights.size() + " insight(s) generated"));
    }

    @RequestMapping(value = "/{id}/dismiss", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ApiResponse<Map<String, String>>> dismissInsight(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        insightsService.dismissInsight(id, principal != null ? principal.getId() : null);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "dismissed")));
    }
}
