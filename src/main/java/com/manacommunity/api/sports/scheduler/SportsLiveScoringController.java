package com.manacommunity.api.sports.scheduler;

import com.manacommunity.api.sports.dto.BallEventRequest;
import com.manacommunity.api.sports.dto.BallEventResponse;
import com.manacommunity.api.sports.dto.SportsLiveMatchStateResponse;
import com.manacommunity.api.sports.dto.SportsMatchDeltaEvent;
import com.manacommunity.api.sports.repository.SportsMatchEventOutboxRepository;
import com.manacommunity.api.sports.service.SportsLiveScoreCacheService;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.platform.websocket.SportsMatchRedisPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SportsLiveScoringController {

    private final SportsLiveScoringService liveScoringService;
    private final SportsLiveScoreCacheService liveScoreCacheService;
    private final SportsMatchRedisPublisher redisPublisher;
    private final SportsMatchEventOutboxRepository outboxRepository;

    @MessageMapping("/match/{matchId}/ball")
    public void handleBallEvent(@DestinationVariable Long matchId, BallEventRequest req, Principal principal) {
        if (principal == null) throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        Long userId = Long.parseLong(principal.getName());
        BallEventResponse response = liveScoringService.recordBall(req, userId);
        SportsLiveMatchStateResponse state = liveScoringService.getMatchState(matchId);

        liveScoreCacheService.setLiveSnapshot(matchId, state);
        SportsMatchDeltaEvent delta = buildDeltaEvent(matchId, "BALL_SCORED", response, state);
        redisPublisher.publishEvent(delta, matchId);
    }

    @MessageMapping("/match/{matchId}/undo")
    public void handleUndo(@DestinationVariable Long matchId, Map<String, Integer> payload, Principal principal) {
        if (principal == null) throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        Integer inningsNumber = payload != null ? payload.getOrDefault("inningsNumber", 1) : 1;
        BallEventResponse response = liveScoringService.undoLastBall(matchId, inningsNumber);
        SportsLiveMatchStateResponse state = liveScoringService.getMatchState(matchId);

        liveScoreCacheService.setLiveSnapshot(matchId, state);
        SportsMatchDeltaEvent delta = buildDeltaEvent(matchId, "UNDO_BALL", response, state);
        redisPublisher.publishEvent(delta, matchId);
    }

    @MessageMapping("/match/{matchId}/startInnings")
    public void handleStartInnings(@DestinationVariable Long matchId, Map<String, Integer> payload, Principal principal) {
        if (principal == null) throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        Integer inningsNumber = payload != null ? payload.getOrDefault("inningsNumber", 1) : 1;
        liveScoringService.startInnings(matchId, inningsNumber);
        SportsLiveMatchStateResponse state = liveScoringService.getMatchState(matchId);

        liveScoreCacheService.setLiveSnapshot(matchId, state);
        SportsMatchDeltaEvent delta = buildDeltaEvent(matchId, "START_INNINGS", null, state);
        redisPublisher.publishEvent(delta, matchId);
    }

    @GetMapping("/api/tournament/match/{matchId}/live")
    public SportsLiveMatchStateResponse getLiveState(@PathVariable Long matchId) {
        return liveScoreCacheService.getLiveSnapshot(matchId, () -> liveScoringService.getMatchState(matchId));
    }

    /**
     * GET /api/tournament/match/{matchId}/events?afterSequence=155
     * Returns missed live scoring events for client reconnect / gap recovery.
     */
    @GetMapping("/api/tournament/match/{matchId}/events")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SportsMatchEventOutbox>> getMissedEvents(
            @PathVariable Long matchId,
            @RequestParam(defaultValue = "0") Long afterSequence) {
        log.info("Fetching missed scoring events for matchId={} afterSequence={}", matchId, afterSequence);
        List<SportsMatchEventOutbox> missed = outboxRepository
                .findByMatchIdAndSequenceNoGreaterThanOrderBySequenceNoAsc(matchId, afterSequence);
        return ResponseEntity.ok(missed);
    }

    /**
     * GET /api/sports/matches/{matchId}/balls
     * Returns all non-undone ball events for a match, optionally filtered by innings.
     * Used by cricket analytics charts (Wagon Wheel, Manhattan, Worm, Pitch Map).
     */
    @GetMapping("/api/sports/matches/{matchId}/balls")
    @PreAuthorize("isAuthenticated()")
    public List<BallEventResponse> getBallEvents(
            @PathVariable Long matchId,
            @RequestParam(required = false) Integer innings) {
        log.info("GET ball events matchId={} innings={}", matchId, innings);
        return liveScoringService.getBallEvents(matchId, innings);
    }

    @PostMapping("/api/tournament/match/{matchId}/ball")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SPORTS_REFEREE','SUPER_ADMIN')")
    public BallEventResponse recordBallRest(@PathVariable Long matchId,
                                            @Valid @RequestBody BallEventRequest req,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        log.info("Recording ball event matchId={} userId={}", matchId, userId);
        BallEventResponse response = liveScoringService.recordBall(req, userId);
        SportsLiveMatchStateResponse state = liveScoringService.getMatchState(matchId);

        liveScoreCacheService.setLiveSnapshot(matchId, state);
        SportsMatchDeltaEvent delta = buildDeltaEvent(matchId, "BALL_SCORED", response, state);
        redisPublisher.publishEvent(delta, matchId);
        return response;
    }

    @PostMapping("/api/tournament/match/{matchId}/undo")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SPORTS_REFEREE','SUPER_ADMIN')")
    public BallEventResponse undoBallRest(@PathVariable Long matchId,
                                          @RequestParam(defaultValue = "1") Integer inningsNumber,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        log.info("Undoing last ball matchId={} inningsNumber={} userId={}", matchId, inningsNumber, userId);
        BallEventResponse response = liveScoringService.undoLastBall(matchId, inningsNumber);
        SportsLiveMatchStateResponse state = liveScoringService.getMatchState(matchId);

        liveScoreCacheService.setLiveSnapshot(matchId, state);
        SportsMatchDeltaEvent delta = buildDeltaEvent(matchId, "UNDO_BALL", response, state);
        redisPublisher.publishEvent(delta, matchId);
        return response;
    }

    private SportsMatchDeltaEvent buildDeltaEvent(Long matchId, String eventType, BallEventResponse ball, SportsLiveMatchStateResponse state) {
        long seq = liveScoreCacheService.getNextSequence(matchId);

        SportsMatchDeltaEvent.ScoreSummary scoreSummary = null;
        SportsMatchDeltaEvent.LastBallSummary lastBallSummary = null;
        SportsMatchDeltaEvent.StrikerSummary strikerSummary = null;
        SportsMatchDeltaEvent.NonStrikerSummary nonStrikerSummary = null;
        SportsMatchDeltaEvent.BowlerSummary bowlerSummary = null;

        if (ball != null) {
            lastBallSummary = new SportsMatchDeltaEvent.LastBallSummary(
                    ball.ballNumber(),
                    ball.overNumber(),
                    ball.deliveryNumber(),
                    ball.runsScored(),
                    ball.extrasRuns(),
                    ball.extrasType(),
                    ball.isBoundary(),
                    ball.isSix(),
                    ball.isWicket(),
                    ball.dismissalType(),
                    ball.commentary()
            );
        }

        if (state != null) {
            var activeInnings = state.currentInnings() != null && state.currentInnings() == 2 && state.innings2() != null
                    ? state.innings2() : state.innings1();
            if (activeInnings != null) {
                scoreSummary = new SportsMatchDeltaEvent.ScoreSummary(
                        state.teamAName(),
                        state.teamBName(),
                        state.innings1() != null ? state.innings1().totalRuns() + "/" + state.innings1().totalWickets() + " (" + state.innings1().totalOvers() + " ov)" : "0/0",
                        state.innings2() != null ? state.innings2().totalRuns() + "/" + state.innings2().totalWickets() + " (" + state.innings2().totalOvers() + " ov)" : null,
                        activeInnings.totalRuns(),
                        activeInnings.totalWickets(),
                        activeInnings.totalOvers(),
                        activeInnings.runRate(),
                        state.target(),
                        null
                );

                if (activeInnings.batters() != null) {
                    for (var b : activeInnings.batters()) {
                        if (Objects.equals(b.playerId(), state.batsmanOnStrikeId())) {
                            strikerSummary = new SportsMatchDeltaEvent.StrikerSummary(
                                    b.playerId(), b.playerName(), b.runs(), b.balls(), b.fours(), b.sixes(), b.strikeRate()
                            );
                        } else if (Objects.equals(b.playerId(), state.batsmanNonStrikeId())) {
                            nonStrikerSummary = new SportsMatchDeltaEvent.NonStrikerSummary(
                                    b.playerId(), b.playerName(), b.runs(), b.balls()
                            );
                        }
                    }
                }

                if (activeInnings.bowlers() != null) {
                    for (var bw : activeInnings.bowlers()) {
                        if (Objects.equals(bw.playerId(), state.currentBowlerId())) {
                            bowlerSummary = new SportsMatchDeltaEvent.BowlerSummary(
                                    bw.playerId(), bw.playerName(), bw.overs(), bw.maidens(), bw.runs(), bw.wickets(), bw.economy()
                            );
                            break;
                        }
                    }
                }
            }
        }

        SportsMatchDeltaEvent delta = new SportsMatchDeltaEvent(
                matchId,
                seq,
                eventType,
                "CRICKET",
                state != null ? state.status() : "LIVE",
                state != null ? state.currentInnings() : 1,
                scoreSummary,
                lastBallSummary,
                strikerSummary,
                nonStrikerSummary,
                bowlerSummary,
                ball != null ? ball.commentary() : eventType,
                Instant.now()
        );

        liveScoreCacheService.recordOutboxEvent(matchId, seq, eventType, delta);
        return delta;
    }
}
