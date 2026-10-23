package com.manacommunity.api.serviceplatform.analytics.controller;

import com.manacommunity.api.serviceplatform.analytics.dto.PlatformAnalyticsResponse;
import com.manacommunity.api.serviceplatform.analytics.service.PlatformAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/service-platform/analytics")
@RequiredArgsConstructor
@Tag(name = "Service Analytics", description = "Community services platform reporting and metrics APIs")
public class PlatformAnalyticsController {

    private final PlatformAnalyticsService analyticsService;

    @GetMapping("/community/{communityId}")
    @Operation(summary = "Get platform performance analytics for a community")
    public ResponseEntity<PlatformAnalyticsResponse> getCommunityAnalytics(@PathVariable Long communityId) {
        return ResponseEntity.ok(analyticsService.getCommunityAnalytics(communityId));
    }
}
