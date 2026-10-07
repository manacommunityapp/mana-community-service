package com.manacommunity.api.intelligence.controller;

import com.manacommunity.api.intelligence.dto.*;
import com.manacommunity.api.intelligence.service.ManaIntelligenceService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/graph", "/api/api/graph", "/graph"})
@RequiredArgsConstructor
public class ManaIntelligenceController {

    private final ManaIntelligenceService intelligenceService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/recommendations/feed")
    public ResponseEntity<Map<String, Object>> getFeed(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        List<RecommendationCardDto> feed = intelligenceService.getPersonalizedFeed(user);
        return ResponseEntity.ok(Map.of("recommendations", feed, "total", feed.size()));
    }

    @GetMapping("/discover/search")
    public ResponseEntity<List<CommunityProfileDto>> searchProfiles(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tower,
            @RequestParam(required = false) String skill,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(intelligenceService.searchDiscoverProfiles(user, q, tower, skill));
    }

    @GetMapping("/omnisearch")
    public ResponseEntity<OmniSearchResponseDto> omniSearch(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "6") int limit,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(intelligenceService.omniSearch(user, q, limit));
    }

    @GetMapping("/discover/skills")
    public ResponseEntity<List<Map<String, Object>>> getSkills(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(intelligenceService.getTopSkills(user));
    }

    @PutMapping("/privacy/visibility")
    public ResponseEntity<CommunityProfileDto> updateVisibility(
            @Valid @RequestBody UpdateVisibilityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(intelligenceService.updateVisibility(user, request));
    }
}
