package com.manacommunity.api.controller;

import com.manacommunity.api.dto.scheduler.SportsRaceResultRequest;
import com.manacommunity.api.dto.scheduler.SportsRaceResultResponse;
import com.manacommunity.api.service.scheduler.SportsRaceResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournament/match/race")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SportsRaceResultController {

    private final SportsRaceResultService raceService;

    @PostMapping("/result")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<SportsRaceResultResponse> recordResult(@RequestBody SportsRaceResultRequest request) {
        return ResponseEntity.ok(raceService.recordResult(request));
    }

    @GetMapping("/{matchId}/results")
    public ResponseEntity<List<SportsRaceResultResponse>> getResults(@PathVariable Long matchId) {
        return ResponseEntity.ok(raceService.getResults(matchId));
    }

    @GetMapping("/{matchId}/heat/{heatNumber}")
    public ResponseEntity<List<SportsRaceResultResponse>> getHeatResults(
            @PathVariable Long matchId, @PathVariable Integer heatNumber) {
        return ResponseEntity.ok(raceService.getHeatResults(matchId, heatNumber));
    }

    @GetMapping("/player/{playerId}/history")
    public ResponseEntity<List<SportsRaceResultResponse>> getPlayerRaceHistory(@PathVariable Long playerId) {
        return ResponseEntity.ok(raceService.getPlayerRaceHistory(playerId));
    }

    @PostMapping("/{matchId}/recalculate-ranks")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<SportsRaceResultResponse>> recalculateRanks(@PathVariable Long matchId) {
        raceService.updateRanks(matchId);
        return ResponseEntity.ok(raceService.getResults(matchId));
    }
}
