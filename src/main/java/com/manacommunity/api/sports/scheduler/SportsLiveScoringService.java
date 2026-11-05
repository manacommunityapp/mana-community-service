package com.manacommunity.api.sports.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.sports.dto.BallEventRequest;
import com.manacommunity.api.sports.dto.BallEventResponse;
import com.manacommunity.api.sports.dto.SportsLiveMatchStateResponse;
import com.manacommunity.api.sports.dto.SportsLiveMatchStateResponse.BatterState;
import com.manacommunity.api.sports.dto.SportsLiveMatchStateResponse.BowlerState;
import com.manacommunity.api.sports.dto.SportsLiveMatchStateResponse.InningsState;
import com.manacommunity.api.sports.dto.SportsMatchResultRequest;
import com.manacommunity.api.sports.model.DismissalType;
import com.manacommunity.api.sports.model.MatchStatus;
import com.manacommunity.api.sports.model.SportsAuctionPlayer;
import com.manacommunity.api.sports.model.SportsAuctionTeam;
import com.manacommunity.api.sports.repository.SportsAuctionPlayerRepository;
import com.manacommunity.api.sports.repository.SportsMatchBallEventRepository;
import com.manacommunity.api.sports.repository.SportsScoringCommandRepository;
import com.manacommunity.api.sports.repository.SportsScoringCorrectionRepository;
import com.manacommunity.api.sports.repository.SportsTournamentMatchRepository;
import com.manacommunity.api.sports.repository.SportsScoringConfigRepository;
import com.manacommunity.api.sports.service.SportsScoringAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsLiveScoringService {

    private final SportsMatchBallEventRepository ballRepo;
    private final SportsTournamentMatchRepository matchRepo;
    private final SportsAuctionPlayerRepository playerRepo;
    private final SportsScoringCommandRepository commandRepo;
    private final SportsScoringCorrectionRepository correctionRepo;
    private final SportsScoringAuthorizationService authService;
    private final SportsScoringConfigRepository scoringConfigRepo;
    private final SportsBracketGenerator bracketGenerator;
    private final SportsTournamentResultNotifier resultNotifier;
    private final ObjectMapper objectMapper;

    @Transactional
    public BallEventResponse recordBall(BallEventRequest req, Long userId) {
        log.info("Recording ball event matchId={} innings={} batsmanId={} bowlerId={} userId={}",
                req.matchId(), req.inningsNumber(), req.batsmanId(), req.bowlerId(), userId);

        // 1. Idempotency check: if commandId was previously processed, return snapshot
        if (req.commandId() != null) {
            Optional<SportsScoringCommand> existingCmd = commandRepo.findByCommandId(req.commandId());
            if (existingCmd.isPresent() && existingCmd.get().getResponseSnapshot() != null) {
                log.info("Idempotent hit for commandId={}", req.commandId());
                try {
                    return objectMapper.readValue(existingCmd.get().getResponseSnapshot(), BallEventResponse.class);
                } catch (JsonProcessingException e) {
                    log.warn("Failed to deserialize cached response for commandId={}", req.commandId(), e);
                }
            }
        }

        // 2. Validate scorer authorization
        if (userId != null) {
            authService.validateScorerForMatch(req.matchId(), userId);
        }

        SportsTournamentMatch match = matchRepo.findById(req.matchId())
                .orElseThrow(() -> new ResourceNotFoundException("SportsTournamentMatch", req.matchId()));

        if (match.getStatus() != MatchStatus.LIVE) {
            match.setStatus(MatchStatus.LIVE);
            if (match.getStartedAt() == null) match.setStartedAt(LocalDateTime.now());
            matchRepo.save(match);
        }

        List<SportsMatchBallEvent> existing = ballRepo.findByMatchIdAndInningsNumberAndIsUndoneFalseOrderByDeliveryNumber(
                req.matchId(), req.inningsNumber());

        int deliveryNumber = existing.isEmpty() ? 1 : existing.get(existing.size() - 1).getDeliveryNumber() + 1;

        int prevRuns = existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getTotalRuns();
        int prevWickets = existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getTotalWickets();

        int ballRuns = (req.runsScored() != null ? req.runsScored() : 0)
                     + (req.extrasRuns() != null ? req.extrasRuns() : 0);
        int newTotalRuns = prevRuns + ballRuns;

        boolean isWicket = Boolean.TRUE.equals(req.isWicket());
        int newTotalWickets = prevWickets + (isWicket ? 1 : 0);

        boolean isLegalDelivery = isLegalDelivery(req.extrasType());
        int legalBalls = countLegalBalls(existing) + (isLegalDelivery ? 1 : 0);
        int overNumber = legalBalls > 0 ? (legalBalls - 1) / 6 : 0;
        int ballInOver = legalBalls > 0 ? ((legalBalls - 1) % 6) + 1 : 0;
        String totalOvers = overNumber + "." + ballInOver;

        SportsAuctionPlayer batsman = playerRepo.findById(req.batsmanId())
                .orElseThrow(() -> new ResourceNotFoundException("Player", req.batsmanId()));
        SportsAuctionPlayer bowler = playerRepo.findById(req.bowlerId())
                .orElseThrow(() -> new ResourceNotFoundException("Player", req.bowlerId()));
        SportsAuctionPlayer nonStriker = req.nonStrikerId() != null
                ? playerRepo.findById(req.nonStrikerId()).orElse(null) : null;
        SportsAuctionPlayer dismissedPlayer = req.dismissedPlayerId() != null
                ? playerRepo.findById(req.dismissedPlayerId()).orElse(null) : null;
        SportsAuctionPlayer fielder = req.fielderId() != null
                ? playerRepo.findById(req.fielderId()).orElse(null) : null;

        SportsMatchBallEvent event = SportsMatchBallEvent.builder()
                .match(match)
                .commandId(req.commandId())
                .inningsNumber(req.inningsNumber())
                .overNumber(overNumber)
                .ballNumber(ballInOver)
                .deliveryNumber(deliveryNumber)
                .runsScored(req.runsScored() != null ? req.runsScored() : 0)
                .isBoundary(Boolean.TRUE.equals(req.isBoundary()))
                .isSix(Boolean.TRUE.equals(req.isSix()))
                .extrasType(req.extrasType())
                .extrasRuns(req.extrasRuns() != null ? req.extrasRuns() : 0)
                .isWicket(isWicket)
                .dismissalType(req.dismissalType() != null ? DismissalType.valueOf(req.dismissalType()) : null)
                .dismissedPlayer(dismissedPlayer)
                .fielder(fielder)
                .batsman(batsman)
                .nonStriker(nonStriker)
                .bowler(bowler)
                .totalRuns(newTotalRuns)
                .totalWickets(newTotalWickets)
                .totalOvers(totalOvers)
                .commentary(req.commentary())
                .isUndone(false)
                .createdBy(userId)
                .build();

        ballRepo.save(event);

        String scoreFormatted = newTotalRuns + "/" + newTotalWickets + " (" + totalOvers + " ov)";
        if (isTeamABatting(match, req.inningsNumber())) {
            match.setScoreTeamA(scoreFormatted);
        } else {
            match.setScoreTeamB(scoreFormatted);
        }

        // ── Auto-completion checks for Overs Quota & Target Chase ──
        int oversLimit = 20;
        if (match.getConfig() != null && scoringConfigRepo != null) {
            var scOpt = scoringConfigRepo.findByConfigId(match.getConfig().getId());
            if (scOpt.isPresent() && scOpt.get().getOversPerInnings() != null && scOpt.get().getOversPerInnings() > 0) {
                oversLimit = scOpt.get().getOversPerInnings();
            }
        }
        if (match.getMatchConfigOverrideJson() != null && !match.getMatchConfigOverrideJson().isBlank()) {
            try {
                Map<?, ?> override = objectMapper.readValue(match.getMatchConfigOverrideJson(), Map.class);
                if (override.get("oversPerInnings") != null) {
                    oversLimit = ((Number) override.get("oversPerInnings")).intValue();
                }
            } catch (Exception ignored) {}
        }

        int totalLegalDeliveries = 0;
        for (SportsMatchBallEvent b : existing) {
            if (b.getExtrasType() == null || (!"WIDE".equalsIgnoreCase(b.getExtrasType()) && !"NO_BALL".equalsIgnoreCase(b.getExtrasType()))) {
                totalLegalDeliveries++;
            }
        }
        if (req.extrasType() == null || (!"WIDE".equalsIgnoreCase(req.extrasType()) && !"NO_BALL".equalsIgnoreCase(req.extrasType()))) {
            totalLegalDeliveries++;
        }

        // Target reached in Innings 2?
        if (req.inningsNumber() == 2) {
            List<SportsMatchBallEvent> inn1Balls = ballRepo.findByMatchIdAndInningsNumberAndIsUndoneFalseOrderByDeliveryNumber(match.getId(), 1);
            int targetRuns = inn1Balls.isEmpty() ? 0 : (inn1Balls.get(inn1Balls.size() - 1).getTotalRuns() + 1);
            if (newTotalRuns >= targetRuns && targetRuns > 0) {
                match.setStatus(MatchStatus.COMPLETED);
                match.setCompletedAt(LocalDateTime.now());
                SportsAuctionTeam chasingTeam = isTeamABatting(match, 2) ? match.getTeamA() : match.getTeamB();
                match.setWinner(chasingTeam);
                match.setResultType(com.manacommunity.api.sports.model.ResultType.WIN);
                int remainingWickets = 10 - newTotalWickets;
                match.setWinMargin(chasingTeam.getTeamName() + " won by " + remainingWickets + " wickets");
            } else if (totalLegalDeliveries >= oversLimit * 6 || newTotalWickets >= 10) {
                // Overs limit reached or all out in 2nd innings
                match.setStatus(MatchStatus.COMPLETED);
                match.setCompletedAt(LocalDateTime.now());
                SportsAuctionTeam teamInn1 = isTeamABatting(match, 1) ? match.getTeamA() : match.getTeamB();
                SportsAuctionTeam teamInn2 = isTeamABatting(match, 2) ? match.getTeamA() : match.getTeamB();
                int inn1Runs = inn1Balls.isEmpty() ? 0 : inn1Balls.get(inn1Balls.size() - 1).getTotalRuns();
                if (newTotalRuns > inn1Runs) {
                    match.setWinner(teamInn2);
                    match.setResultType(com.manacommunity.api.sports.model.ResultType.WIN);
                    match.setWinMargin(teamInn2.getTeamName() + " won by " + (10 - newTotalWickets) + " wickets");
                } else if (newTotalRuns < inn1Runs) {
                    match.setWinner(teamInn1);
                    match.setResultType(com.manacommunity.api.sports.model.ResultType.WIN);
                    match.setWinMargin(teamInn1.getTeamName() + " won by " + (inn1Runs - newTotalRuns) + " runs");
                } else {
                    match.setResultType(com.manacommunity.api.sports.model.ResultType.DRAW);
                    match.setWinMargin("Match Tied");
                }
            }
        }

        SportsTournamentMatch saved = matchRepo.save(match);
        if (saved != null) {
            match = saved;
        }

        if (match != null && match.getStatus() == MatchStatus.COMPLETED) {
            try {
                bracketGenerator.applyResult(new SportsMatchResultRequest(
                    match.getId(),
                    match.getWinner() != null ? match.getWinner().getId() : null,
                    match.getScoreTeamA(),
                    match.getScoreTeamB(),
                    match.getWinMargin(),
                    null, null, null, null,
                    match.getWinner() != null ? match.getWinner().getTeamName() : match.getWinnerName(),
                    null
                ));
                resultNotifier.notifyMatchResult(match.getId());
            } catch (Exception ex) {
                log.warn("Failed to advance bracket or notify on live match completion: {}", ex.getMessage());
            }
        }

        BallEventResponse response = toBallEventResponse(event);

        // Store command for idempotency
        if (req.commandId() != null) {
            String payloadJson = null;
            String responseJson = null;
            try {
                payloadJson = objectMapper.writeValueAsString(req);
                responseJson = objectMapper.writeValueAsString(response);
            } catch (JsonProcessingException ignored) {}

            SportsScoringCommand cmd = SportsScoringCommand.builder()
                    .commandId(req.commandId())
                    .match(match)
                    .sequenceNo((long) deliveryNumber)
                    .commandType("RECORD_BALL")
                    .payload(payloadJson)
                    .responseSnapshot(responseJson)
                    .appliedBy(userId)
                    .appliedAt(LocalDateTime.now())
                    .build();
            commandRepo.save(cmd);
        }

        return response;
    }

    @Transactional
    public BallEventResponse undoLastBall(Long matchId, Integer inningsNumber) {
        log.info("Undoing last ball matchId={} innings={}", matchId, inningsNumber);
        List<SportsMatchBallEvent> balls = ballRepo.findByMatchIdAndInningsNumberAndIsUndoneFalseOrderByDeliveryNumber(
                matchId, inningsNumber);
        if (balls.isEmpty()) throw new InvalidInputException("No balls to undo for this innings.");

        SportsMatchBallEvent last = balls.get(balls.size() - 1);
        last.setIsUndone(true);
        ballRepo.save(last);

        SportsTournamentMatch match = last.getMatch();
        if (balls.size() > 1) {
            SportsMatchBallEvent prev = balls.get(balls.size() - 2);
            String score = prev.getTotalRuns() + "/" + prev.getTotalWickets() + " (" + prev.getTotalOvers() + " ov)";
            if (isTeamABatting(match, inningsNumber)) {
                match.setScoreTeamA(score);
            } else {
                match.setScoreTeamB(score);
            }
        } else {
            // Sole ball in innings was undone: reset to 0/0 (0.0 ov)
            String resetScore = "0/0 (0.0 ov)";
            if (isTeamABatting(match, inningsNumber)) {
                match.setScoreTeamA(resetScore);
            } else {
                match.setScoreTeamB(resetScore);
            }
        }
        matchRepo.save(match);

        // Record audit trail of correction
        correctionRepo.save(SportsScoringCorrection.builder()
                .match(match)
                .originalEventId(last.getId())
                .correctionType("UNDO_BALL")
                .reason("Undid delivery #" + last.getDeliveryNumber() + " in innings " + inningsNumber)
                .correctedBy(last.getCreatedBy())
                .createdAt(LocalDateTime.now())
                .build());

        return toBallEventResponse(last);
    }

    @Transactional
    public void startInnings(Long matchId, Integer inningsNumber) {
        log.info("Starting innings matchId={} innings={}", matchId, inningsNumber);
        SportsTournamentMatch match = matchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsTournamentMatch", matchId));
        if (match.getStatus() != MatchStatus.LIVE) {
            match.setStatus(MatchStatus.LIVE);
            if (match.getStartedAt() == null) match.setStartedAt(LocalDateTime.now());
            matchRepo.save(match);
        }
    }

    @Transactional(readOnly = true)
    public SportsLiveMatchStateResponse getMatchState(Long matchId) {
        SportsTournamentMatch match = matchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsTournamentMatch", matchId));

        List<SportsMatchBallEvent> allBalls = ballRepo.findByMatchIdAndIsUndoneFalseOrderByDeliveryNumber(matchId);
        List<SportsMatchBallEvent> inn1Balls = allBalls.stream().filter(b -> b.getInningsNumber() == 1).toList();
        List<SportsMatchBallEvent> inn2Balls = allBalls.stream().filter(b -> b.getInningsNumber() == 2).toList();

        int currentInnings = inn2Balls.isEmpty() ? 1 : 2;
        List<SportsMatchBallEvent> currentBalls = currentInnings == 1 ? inn1Balls : inn2Balls;

        Long batsmanOnStrikeId = null;
        String batsmanOnStrikeName = null;
        Long batsmanNonStrikeId = null;
        String batsmanNonStrikeName = null;
        Long currentBowlerId = null;
        String currentBowlerName = null;

        if (!currentBalls.isEmpty()) {
            SportsMatchBallEvent lastBall = currentBalls.get(currentBalls.size() - 1);
            batsmanOnStrikeId = lastBall.getBatsman().getId();
            batsmanOnStrikeName = lastBall.getBatsman().getPlayerName();
            if (lastBall.getNonStriker() != null) {
                batsmanNonStrikeId = lastBall.getNonStriker().getId();
                batsmanNonStrikeName = lastBall.getNonStriker().getPlayerName();
            }
            currentBowlerId = lastBall.getBowler().getId();
            currentBowlerName = lastBall.getBowler().getPlayerName();
        }

        Integer target = null;
        if (!inn1Balls.isEmpty() && !inn2Balls.isEmpty()) {
            target = inn1Balls.get(inn1Balls.size() - 1).getTotalRuns() + 1;
        }

        List<BallEventResponse> recentBalls = currentBalls.stream()
                .skip(Math.max(0, currentBalls.size() - 12))
                .map(this::toBallEventResponse).toList();

        SportsAuctionTeam teamInn1 = isTeamABatting(match, 1) ? match.getTeamA() : match.getTeamB();
        SportsAuctionTeam teamInn2 = isTeamABatting(match, 2) ? match.getTeamA() : match.getTeamB();

        Integer oversLimit = 20;
        Integer maxOversPerBowler = 4;
        Integer ballsPerOver = 6;

        if (match.getConfig() != null && scoringConfigRepo != null) {
            Optional<SportsScoringConfig> scOpt = scoringConfigRepo.findByConfigId(match.getConfig().getId());
            if (scOpt.isPresent()) {
                SportsScoringConfig sc = scOpt.get();
                if (sc.getOversPerInnings() != null && sc.getOversPerInnings() > 0) oversLimit = sc.getOversPerInnings();
                if (sc.getMaxOversPerBowler() != null && sc.getMaxOversPerBowler() > 0) maxOversPerBowler = sc.getMaxOversPerBowler();
                if (sc.getBallsPerOver() != null && sc.getBallsPerOver() > 0) ballsPerOver = sc.getBallsPerOver();
            }
        }
        if (match.getMatchConfigOverrideJson() != null && !match.getMatchConfigOverrideJson().isBlank()) {
            try {
                Map<?, ?> override = objectMapper.readValue(match.getMatchConfigOverrideJson(), Map.class);
                if (override.get("oversPerInnings") != null) {
                    oversLimit = ((Number) override.get("oversPerInnings")).intValue();
                }
                if (override.get("maxOversPerBowler") != null) {
                    maxOversPerBowler = ((Number) override.get("maxOversPerBowler")).intValue();
                }
            } catch (Exception ignored) {}
        }

        return new SportsLiveMatchStateResponse(
                matchId,
                match.getStatus().name(),
                currentInnings,
                buildInningsState(inn1Balls, teamInn1),
                buildInningsState(inn2Balls, teamInn2),
                batsmanOnStrikeId, batsmanOnStrikeName,
                batsmanNonStrikeId, batsmanNonStrikeName,
                currentBowlerId, currentBowlerName,
                recentBalls,
                match.getTeamA() != null ? match.getTeamA().getTeamName() : "TBD",
                match.getTeamB() != null ? match.getTeamB().getTeamName() : "TBD",
                match.getTeamA() != null ? match.getTeamA().getColorHex() : "#555",
                match.getTeamB() != null ? match.getTeamB().getColorHex() : "#555",
                match.getTeamA() != null ? match.getTeamA().getId() : null,
                match.getTeamB() != null ? match.getTeamB().getId() : null,
                target,
                oversLimit,
                maxOversPerBowler,
                ballsPerOver
        );
    }

    /**
     * Accurately determines whether Team A or Team B is batting in a given innings
     * based on toss winner and toss decision.
     */
    public boolean isTeamABatting(SportsTournamentMatch match, int inningsNumber) {
        if (match.getTeamA() == null || match.getTeamB() == null) {
            return inningsNumber == 1;
        }

        boolean teamAWonToss = match.getTossWinner() != null
                && match.getTossWinner().getId().equals(match.getTeamA().getId());
        boolean teamBWonToss = match.getTossWinner() != null
                && match.getTossWinner().getId().equals(match.getTeamB().getId());

        boolean tossWinnerChoseToBat = match.getTossDecision() == null
                || "BAT".equalsIgnoreCase(match.getTossDecision())
                || "BATTING".equalsIgnoreCase(match.getTossDecision());

        boolean teamABattedFirst;
        if (teamAWonToss) {
            teamABattedFirst = tossWinnerChoseToBat;
        } else if (teamBWonToss) {
            teamABattedFirst = !tossWinnerChoseToBat;
        } else {
            teamABattedFirst = true; // default: Team A bats first
        }

        return inningsNumber == 1 ? teamABattedFirst : !teamABattedFirst;
    }

    private InningsState buildInningsState(List<SportsMatchBallEvent> balls, SportsAuctionTeam team) {
        if (balls.isEmpty()) {
            return new InningsState(
                    team != null ? team.getId() : null,
                    team != null ? team.getTeamName() : "TBD",
                    0, 0, "0.0", "0.00",
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
            );
        }

        SportsMatchBallEvent last = balls.get(balls.size() - 1);
        int totalRuns = last.getTotalRuns();
        int totalWickets = last.getTotalWickets();
        String totalOvers = last.getTotalOvers();

        int legalBalls = countLegalBalls(balls);
        BigDecimal oversDecimal = legalBalls > 0
                ? new BigDecimal(legalBalls).divide(BigDecimal.valueOf(6), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        String runRate = oversDecimal.compareTo(BigDecimal.ZERO) > 0
                ? new BigDecimal(totalRuns).divide(oversDecimal, 2, RoundingMode.HALF_UP).toPlainString()
                : "0.00";

        Map<Long, int[]> batterStats = new LinkedHashMap<>();
        Map<Long, String> batterNames = new LinkedHashMap<>();
        Map<Long, int[]> bowlerStats = new LinkedHashMap<>();
        Map<Long, String> bowlerNames = new LinkedHashMap<>();
        Map<Long, String> dismissals = new HashMap<>();

        for (SportsMatchBallEvent b : balls) {
            Long batId = b.getBatsman().getId();
            batterNames.putIfAbsent(batId, b.getBatsman().getPlayerName());
            int[] bs = batterStats.computeIfAbsent(batId, k -> new int[5]);
            bs[0] += b.getRunsScored();
            if (isLegalDelivery(b.getExtrasType()) || "NO_BALL".equals(b.getExtrasType())) {
                bs[1]++;
            }
            if (Boolean.TRUE.equals(b.getIsBoundary()) && !Boolean.TRUE.equals(b.getIsSix())) bs[2]++;
            if (Boolean.TRUE.equals(b.getIsSix())) bs[3]++;

            if (Boolean.TRUE.equals(b.getIsWicket()) && b.getDismissedPlayer() != null) {
                Long dpId = b.getDismissedPlayer().getId();
                String dt = b.getDismissalType() != null ? b.getDismissalType().name().toLowerCase().replace("_", " ") : "out";
                dismissals.put(dpId, dt);
            }

            Long bowId = b.getBowler().getId();
            bowlerNames.putIfAbsent(bowId, b.getBowler().getPlayerName());
            int[] bw = bowlerStats.computeIfAbsent(bowId, k -> new int[5]);
            bw[0] += b.getRunsScored() + b.getExtrasRuns();
            if (Boolean.TRUE.equals(b.getIsWicket())) bw[1]++;
            if (isLegalDelivery(b.getExtrasType())) bw[2]++;
            if (b.getRunsScored() == 0 && (b.getExtrasRuns() == null || b.getExtrasRuns() == 0) && isLegalDelivery(b.getExtrasType())) bw[3]++;
        }

        List<BatterState> batterList = new ArrayList<>();
        for (var entry : batterStats.entrySet()) {
            Long pid = entry.getKey();
            int[] s = entry.getValue();
            String sr = s[1] > 0 ? new BigDecimal(s[0] * 100).divide(new BigDecimal(s[1]), 2, RoundingMode.HALF_UP).toPlainString() : "0.00";
            boolean isOut = dismissals.containsKey(pid);
            batterList.add(new BatterState(pid, batterNames.get(pid), s[0], s[1], s[2], s[3], sr, isOut, dismissals.get(pid)));
        }

        List<BowlerState> bowlerList = new ArrayList<>();
        for (var entry : bowlerStats.entrySet()) {
            Long pid = entry.getKey();
            int[] s = entry.getValue();
            int completedOvers = s[2] / 6;
            int partialBalls = s[2] % 6;
            String overs = completedOvers + "." + partialBalls;
            BigDecimal od = s[2] > 0 ? new BigDecimal(s[2]).divide(BigDecimal.valueOf(6), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            String eco = od.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal(s[0]).divide(od, 2, RoundingMode.HALF_UP).toPlainString() : "0.00";
            bowlerList.add(new BowlerState(pid, bowlerNames.get(pid), overs, s[0], s[1], 0, eco, s[3]));
        }

        List<String> currentOverBalls = new ArrayList<>();
        for (int i = balls.size() - 1; i >= 0; i--) {
            SportsMatchBallEvent b = balls.get(i);
            if (isLegalDelivery(b.getExtrasType()) && b.getOverNumber() < (legalBalls - 1) / 6) break;
            currentOverBalls.add(0, formatBallShort(b));
        }

        return new InningsState(
                team != null ? team.getId() : null,
                team != null ? team.getTeamName() : "TBD",
                totalRuns, totalWickets, totalOvers, runRate,
                batterList, bowlerList, currentOverBalls
        );
    }

    private String formatBallShort(SportsMatchBallEvent b) {
        if (Boolean.TRUE.equals(b.getIsWicket())) return "W";
        if (b.getExtrasType() != null) {
            return switch (b.getExtrasType()) {
                case "WIDE" -> b.getExtrasRuns() + "wd";
                case "NO_BALL" -> b.getExtrasRuns() + "nb";
                case "BYE" -> b.getExtrasRuns() + "b";
                case "LEG_BYE" -> b.getExtrasRuns() + "lb";
                default -> String.valueOf(b.getRunsScored());
            };
        }
        if (Boolean.TRUE.equals(b.getIsSix())) return "6";
        if (Boolean.TRUE.equals(b.getIsBoundary())) return "4";
        return String.valueOf(b.getRunsScored());
    }

    private boolean isLegalDelivery(String extrasType) {
        return extrasType == null || (!extrasType.equals("WIDE") && !extrasType.equals("NO_BALL"));
    }

    private int countLegalBalls(List<SportsMatchBallEvent> balls) {
        return (int) balls.stream().filter(b -> isLegalDelivery(b.getExtrasType())).count();
    }

    private BallEventResponse toBallEventResponse(SportsMatchBallEvent e) {
        return new BallEventResponse(
                e.getId(), e.getMatch().getId(),
                e.getInningsNumber(), e.getOverNumber(), e.getBallNumber(), e.getDeliveryNumber(),
                e.getRunsScored(), e.getIsBoundary(), e.getIsSix(),
                e.getExtrasType(), e.getExtrasRuns(),
                e.getIsWicket(),
                e.getDismissalType() != null ? e.getDismissalType().name() : null,
                e.getDismissedPlayer() != null ? e.getDismissedPlayer().getId() : null,
                e.getDismissedPlayer() != null ? e.getDismissedPlayer().getPlayerName() : null,
                e.getFielder() != null ? e.getFielder().getId() : null,
                e.getFielder() != null ? e.getFielder().getPlayerName() : null,
                e.getBatsman().getId(), e.getBatsman().getPlayerName(),
                e.getNonStriker() != null ? e.getNonStriker().getId() : null,
                e.getNonStriker() != null ? e.getNonStriker().getPlayerName() : null,
                e.getBowler().getId(), e.getBowler().getPlayerName(),
                e.getTotalRuns(), e.getTotalWickets(), e.getTotalOvers(),
                e.getCommentary(),
                e.getCreatedAt() != null ? e.getCreatedAt().toString() : null
        );
    }
}
