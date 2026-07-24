package com.manacommunity.api.controller;

import com.manacommunity.api.dto.EngagementScoreResponse;
import com.manacommunity.api.dto.FeedAnalyticsResponse;
import com.manacommunity.api.dto.TrendingResponse;
import com.manacommunity.api.service.EngagementService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/engagement")
@RequiredArgsConstructor
public class EngagementController {

    private final LoggedInUserService loggedInUserService;
    private final EngagementService engagementService;

    @GetMapping("/score")
    public ResponseEntity<EngagementScoreResponse> getMyScore(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(engagementService.getEngagementScore(currentUser));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<EngagementScoreResponse>> getLeaderboard(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "20") int limit) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(engagementService.getLeaderboard(currentUser, Math.min(limit, 100)));
    }

    @GetMapping("/trending")
    public ResponseEntity<List<TrendingResponse>> getTrending(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "10") int limit) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(engagementService.getTrending(currentUser, Math.min(limit, 50)));
    }

    @GetMapping("/analytics")
    public ResponseEntity<FeedAnalyticsResponse> getAnalytics(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser currentUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(engagementService.getAnalytics(currentUser));
    }
}
