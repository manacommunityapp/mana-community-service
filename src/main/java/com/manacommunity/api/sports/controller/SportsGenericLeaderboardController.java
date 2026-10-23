package com.manacommunity.api.sports.controller;

import com.manacommunity.api.sports.dto.SportsGenericLeaderboardEntry;
import com.manacommunity.api.sports.scheduler.SportsGenericLeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournament/{configId}/leaderboard/generic")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SportsGenericLeaderboardController {

    private final SportsGenericLeaderboardService leaderboardService;

    @GetMapping
    public ResponseEntity<List<SportsGenericLeaderboardEntry>> getLeaderboard(
            @PathVariable Long configId,
            @RequestParam(defaultValue = "points") String category) {
        return ResponseEntity.ok(leaderboardService.getLeaderboard(configId, category));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories(@PathVariable Long configId) {
        return ResponseEntity.ok(leaderboardService.getCategories(configId));
    }
}
