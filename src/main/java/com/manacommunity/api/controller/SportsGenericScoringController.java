package com.manacommunity.api.controller;

import com.manacommunity.api.dto.scheduler.*;
import com.manacommunity.api.service.scheduler.SportsGenericScoringService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tournament/match/generic")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SportsGenericScoringController {

    private final SportsGenericScoringService scoringService;
    private final LoggedInUserService loggedInUserService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/score")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<SportsGenericScoreResponse> recordEvent(
            @RequestBody SportsGenericScoreRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        SportsGenericScoreResponse response = scoringService.recordEvent(request, user.getId());
        broadcastState(request.matchId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{matchId}/undo")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<SportsGenericScoreResponse> undoLastEvent(@PathVariable Long matchId) {
        SportsGenericScoreResponse response = scoringService.undoLastEvent(matchId);
        broadcastState(matchId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{matchId}/state")
    public ResponseEntity<SportsGenericMatchStateResponse> getMatchState(@PathVariable Long matchId) {
        return ResponseEntity.ok(scoringService.getMatchState(matchId));
    }

    @PostMapping("/{matchId}/period/{periodNumber}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> completePeriod(
            @PathVariable Long matchId,
            @PathVariable Integer periodNumber) {
        scoringService.completePeriod(matchId, periodNumber);
        broadcastState(matchId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{matchId}/period/{periodNumber}/score")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<SportsPeriodScoreResponse> recordPeriodResult(
            @PathVariable Long matchId,
            @PathVariable Integer periodNumber,
            @RequestBody Map<String, Integer> body) {
        Integer scoreA = body.get("scoreA");
        Integer scoreB = body.get("scoreB");
        SportsPeriodScoreResponse response = scoringService.recordPeriodResult(matchId, periodNumber, scoreA, scoreB);
        broadcastState(matchId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{matchId}/player-stats")
    public ResponseEntity<List<SportsPlayerMatchStatsResponse>> getPlayerMatchStats(@PathVariable Long matchId) {
        return ResponseEntity.ok(scoringService.getPlayerMatchStats(matchId));
    }

    @PostMapping("/scoring-config")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<SportsScoringConfigResponse> saveScoringConfig(@RequestBody SportsScoringConfigRequest request) {
        return ResponseEntity.ok(scoringService.saveScoringConfig(request));
    }

    @GetMapping("/scoring-config/{configId}")
    public ResponseEntity<SportsScoringConfigResponse> getScoringConfig(@PathVariable Long configId) {
        SportsScoringConfigResponse response = scoringService.getScoringConfig(configId);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }

    @GetMapping("/scoring-config/defaults/{sportType}")
    public ResponseEntity<SportsScoringConfigResponse> getDefaultScoringConfig(@PathVariable String sportType) {
        return ResponseEntity.ok(scoringService.getDefaultScoringConfig(sportType));
    }

    @MessageMapping("/generic-match/{matchId}/score")
    public void handleScore(@DestinationVariable Long matchId, SportsGenericScoreRequest request, Principal principal) {
        if (principal == null) throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        Long userId = Long.parseLong(principal.getName());
        scoringService.recordEvent(request, userId);
        broadcastState(matchId);
    }

    @MessageMapping("/generic-match/{matchId}/undo")
    public void handleUndo(@DestinationVariable Long matchId, Principal principal) {
        if (principal == null) throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        scoringService.undoLastEvent(matchId);
        broadcastState(matchId);
    }

    private void broadcastState(Long matchId) {
        SportsGenericMatchStateResponse state = scoringService.getMatchState(matchId);
        messagingTemplate.convertAndSend("/topic/match/" + matchId + "/generic-state", state);
    }
}
