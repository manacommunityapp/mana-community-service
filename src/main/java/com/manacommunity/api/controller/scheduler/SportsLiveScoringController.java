package com.manacommunity.api.controller.scheduler;

import com.manacommunity.api.dto.scheduler.BallEventRequest;
import com.manacommunity.api.dto.scheduler.BallEventResponse;
import com.manacommunity.api.dto.scheduler.SportsLiveMatchStateResponse;
import com.manacommunity.api.service.scheduler.SportsLiveScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
public class SportsLiveScoringController {

    private final SportsLiveScoringService liveScoringService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/match/{matchId}/ball")
    public void handleBallEvent(@DestinationVariable Long matchId, BallEventRequest req, Principal principal) {
        if (principal == null) throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        Long userId = Long.parseLong(principal.getName());
        BallEventResponse response = liveScoringService.recordBall(req, userId);
        messagingTemplate.convertAndSend("/topic/match/" + matchId, response);

        SportsLiveMatchStateResponse state = liveScoringService.getMatchState(matchId);
        messagingTemplate.convertAndSend("/topic/match/" + matchId + "/state", state);
    }

    @MessageMapping("/match/{matchId}/undo")
    public void handleUndo(@DestinationVariable Long matchId, java.util.Map<String, Integer> payload, Principal principal) {
        if (principal == null) throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        Integer inningsNumber = payload.getOrDefault("inningsNumber", 1);
        BallEventResponse response = liveScoringService.undoLastBall(matchId, inningsNumber);
        messagingTemplate.convertAndSend("/topic/match/" + matchId + "/undo", response);

        SportsLiveMatchStateResponse state = liveScoringService.getMatchState(matchId);
        messagingTemplate.convertAndSend("/topic/match/" + matchId + "/state", state);
    }

    @MessageMapping("/match/{matchId}/startInnings")
    public void handleStartInnings(@DestinationVariable Long matchId, java.util.Map<String, Integer> payload, Principal principal) {
        if (principal == null) throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        Integer inningsNumber = payload.getOrDefault("inningsNumber", 1);
        liveScoringService.startInnings(matchId, inningsNumber);

        SportsLiveMatchStateResponse state = liveScoringService.getMatchState(matchId);
        messagingTemplate.convertAndSend("/topic/match/" + matchId + "/state", state);
    }

    @GetMapping("/api/tournament/match/{matchId}/live")
    public SportsLiveMatchStateResponse getLiveState(@PathVariable Long matchId) {
        return liveScoringService.getMatchState(matchId);
    }

    @PostMapping("/api/tournament/match/{matchId}/ball")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SPORTS_REFEREE','SUPER_ADMIN')")
    public BallEventResponse recordBallRest(@PathVariable Long matchId, @RequestBody BallEventRequest req) {
        BallEventResponse response = liveScoringService.recordBall(req, null);
        messagingTemplate.convertAndSend("/topic/match/" + matchId, response);
        SportsLiveMatchStateResponse state = liveScoringService.getMatchState(matchId);
        messagingTemplate.convertAndSend("/topic/match/" + matchId + "/state", state);
        return response;
    }

    @PostMapping("/api/tournament/match/{matchId}/undo")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SPORTS_REFEREE','SUPER_ADMIN')")
    public BallEventResponse undoBallRest(@PathVariable Long matchId, @RequestParam(defaultValue = "1") Integer inningsNumber) {
        BallEventResponse response = liveScoringService.undoLastBall(matchId, inningsNumber);
        messagingTemplate.convertAndSend("/topic/match/" + matchId + "/undo", response);
        SportsLiveMatchStateResponse state = liveScoringService.getMatchState(matchId);
        messagingTemplate.convertAndSend("/topic/match/" + matchId + "/state", state);
        return response;
    }
}
