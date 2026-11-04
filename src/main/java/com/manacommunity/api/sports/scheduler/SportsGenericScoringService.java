package com.manacommunity.api.sports.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.engine.ScoringEngine;
import com.manacommunity.api.sports.engine.ScoringEngineRegistry;
import com.manacommunity.api.sports.model.MatchStatus;
import com.manacommunity.api.sports.model.ResultType;
import com.manacommunity.api.sports.model.SportsAuctionPlayer;
import com.manacommunity.api.sports.model.SportsAuctionTeam;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.service.SportsScoringAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsGenericScoringService {

    private final SportsMatchEventRepository eventRepo;
    private final SportsMatchPeriodScoreRepository periodScoreRepo;
    private final SportsPlayerMatchStatsRepository playerStatsRepo;
    private final SportsScoringConfigRepository scoringConfigRepo;
    private final SportsTournamentMatchRepository matchRepo;
    private final SportsAuctionTeamRepository teamRepo;
    private final SportsAuctionPlayerRepository playerRepo;
    private final SportsScoringCommandRepository commandRepo;
    private final SportsScoringCorrectionRepository correctionRepo;
    private final SportsScoringAuthorizationService authService;
    private final ScoringEngineRegistry engineRegistry;
    private final ObjectMapper objectMapper;

    private static final Set<String> POINT_BASED_SPORTS = Set.of(
            "BADMINTON", "VOLLEYBALL", "TABLE_TENNIS", "TENNIS",
            "PICKLEBALL", "SQUASH", "CARROM", "FOOSBALL",
            "POOL", "BILLIARDS", "SNOOKER", "DART", "BOWLING", "TUG_OF_WAR", "CHESS"
    );

    private static final Set<String> TIMED_SPORTS = Set.of(
            "FOOTBALL", "SOCCER", "BASKETBALL", "KABADDI"
    );

    @Transactional
    public SportsGenericScoreResponse recordEvent(SportsGenericScoreRequest req, Long userId) {
        log.info("Recording generic score event matchId={} teamId={} eventType={} userId={}",
                req.matchId(), req.teamId(), req.eventType(), userId);

        // 1. Idempotency check: if commandId already exists, return snapshot
        if (req.commandId() != null) {
            Optional<SportsScoringCommand> existingCmd = commandRepo.findByCommandId(req.commandId());
            if (existingCmd.isPresent() && existingCmd.get().getResponseSnapshot() != null) {
                log.info("Idempotent hit for generic score commandId={}", req.commandId());
                try {
                    return objectMapper.readValue(existingCmd.get().getResponseSnapshot(), SportsGenericScoreResponse.class);
                } catch (JsonProcessingException e) {
                    log.warn("Failed to deserialize cached response for commandId={}", req.commandId(), e);
                }
            }
        }

        // 2. Authorize scorer
        if (userId != null) {
            authService.validateScorerForMatch(req.matchId(), userId);
        }

        SportsTournamentMatch match = matchRepo.findById(req.matchId())
                .orElseThrow(() -> new ResourceNotFoundException("SportsTournamentMatch", req.matchId()));

        // 3. Validate team membership in match
        boolean isTeamA = match.getTeamA() != null && match.getTeamA().getId().equals(req.teamId());
        boolean isTeamB = match.getTeamB() != null && match.getTeamB().getId().equals(req.teamId());
        if (!isTeamA && !isTeamB) {
            throw new InvalidInputException("Team " + req.teamId() + " does not belong to match " + match.getId());
        }

        SportsAuctionTeam team = teamRepo.findById(req.teamId())
                .orElseThrow(() -> new ResourceNotFoundException("SportsAuctionTeam", req.teamId()));

        SportsAuctionPlayer player = req.playerId() != null
                ? playerRepo.findById(req.playerId()).orElse(null) : null;

        // 4. Sport engine specific validation
        String sportType = resolveSportType(match);
        ScoringEngine engine = engineRegistry.getEngine(sportType);
        engine.validateScoreEvent(match, req);

        if (match.getStatus() != MatchStatus.LIVE) {
            match.setStatus(MatchStatus.LIVE);
            if (match.getStartedAt() == null) match.setStartedAt(LocalDateTime.now());
            matchRepo.save(match);
        }

        SportsMatchEvent event = SportsMatchEvent.builder()
                .match(match)
                .team(team)
                .player(player)
                .commandId(req.commandId())
                .eventType(req.eventType())
                .periodNumber(req.periodNumber())
                .matchMinute(req.matchMinute())
                .pointsAwarded(req.pointsAwarded())
                .description(req.description())
                .secondaryPlayerId(req.secondaryPlayerId())
                .isUndone(false)
                .createdBy(userId)
                .build();

        eventRepo.save(event);

        if (POINT_BASED_SPORTS.contains(sportType) && req.pointsAwarded() > 0) {
            updatePeriodScore(match, req.periodNumber(), req.teamId(), req.pointsAwarded(), sportType);
        }

        updateMatchScore(match, sportType);

        SportsGenericScoreResponse response = toResponse(event);

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
                    .sequenceNo((long) event.getId())
                    .commandType("RECORD_GENERIC_EVENT")
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
    public SportsGenericScoreResponse undoLastEvent(Long matchId) {
        log.info("Undoing last generic score event matchId={}", matchId);
        List<SportsMatchEvent> events = eventRepo.findByMatchIdAndIsUndoneFalseOrderByCreatedAt(matchId);
        if (events.isEmpty()) throw new InvalidInputException("No events to undo for this match.");

        SportsMatchEvent last = events.get(events.size() - 1);
        last.setIsUndone(true);
        eventRepo.save(last);

        SportsTournamentMatch match = last.getMatch();
        String sportType = resolveSportType(match);

        if (POINT_BASED_SPORTS.contains(sportType) && last.getPointsAwarded() > 0) {
            recalculatePeriodScore(match, last.getPeriodNumber(), sportType);
        }

        updateMatchScore(match, sportType);

        // Log correction audit
        correctionRepo.save(SportsScoringCorrection.builder()
                .match(match)
                .originalEventId(last.getId())
                .correctionType("UNDO_GENERIC_EVENT")
                .reason("Undid " + last.getEventType() + " event #" + last.getId())
                .correctedBy(last.getCreatedBy())
                .createdAt(LocalDateTime.now())
                .build());

        return toResponse(last);
    }

    @Transactional(readOnly = true)
    public SportsGenericMatchStateResponse getMatchState(Long matchId) {
        SportsTournamentMatch match = matchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsTournamentMatch", matchId));

        String sportType = resolveSportType(match);
        List<SportsMatchPeriodScore> periods = periodScoreRepo.findByMatchIdOrderByPeriodNumber(matchId);
        List<SportsMatchEvent> events = eventRepo.findByMatchIdAndIsUndoneFalseOrderByCreatedAt(matchId);

        int currentPeriod = periods.isEmpty() ? 1 : periods.get(periods.size() - 1).getPeriodNumber();
        int totalScoreA = 0;
        int totalScoreB = 0;
        int periodsWonA = 0;
        int periodsWonB = 0;

        List<SportsPeriodScoreResponse> periodResponses = new ArrayList<>();
        for (SportsMatchPeriodScore ps : periods) {
            periodResponses.add(new SportsPeriodScoreResponse(
                    ps.getPeriodNumber(), ps.getPeriodLabel(), ps.getScoreTeamA(), ps.getScoreTeamB()
            ));
            if (POINT_BASED_SPORTS.contains(sportType)) {
                if (ps.getScoreTeamA() > ps.getScoreTeamB()) periodsWonA++;
                else if (ps.getScoreTeamB() > ps.getScoreTeamA()) periodsWonB++;
                totalScoreA += ps.getScoreTeamA();
                totalScoreB += ps.getScoreTeamB();
            } else {
                totalScoreA += ps.getScoreTeamA();
                totalScoreB += ps.getScoreTeamB();
            }
        }

        if (TIMED_SPORTS.contains(sportType) && periods.isEmpty()) {
            totalScoreA = countPointsForTeam(events, match.getTeamA() != null ? match.getTeamA().getId() : null);
            totalScoreB = countPointsForTeam(events, match.getTeamB() != null ? match.getTeamB().getId() : null);
        }

        List<SportsGenericScoreResponse> recentEvents = events.stream()
                .skip(Math.max(0, events.size() - 20))
                .map(this::toResponse)
                .toList();

        SportsScoringConfigResponse configResponse = null;
        if (match.getConfig() != null) {
            configResponse = scoringConfigRepo.findByConfigId(match.getConfig().getId())
                    .map(this::toConfigResponse)
                    .orElse(null);
        }

        return new SportsGenericMatchStateResponse(
                matchId,
                match.getStatus().name(),
                sportType,
                match.getTeamA() != null ? match.getTeamA().getId() : null,
                match.getTeamA() != null ? match.getTeamA().getTeamName() : "TBD",
                match.getTeamA() != null ? match.getTeamA().getColorHex() : "#555",
                match.getTeamB() != null ? match.getTeamB().getId() : null,
                match.getTeamB() != null ? match.getTeamB().getTeamName() : "TBD",
                match.getTeamB() != null ? match.getTeamB().getColorHex() : "#555",
                currentPeriod,
                totalScoreA,
                totalScoreB,
                periodsWonA,
                periodsWonB,
                periodResponses,
                recentEvents,
                configResponse
        );
    }

    @Transactional
    public SportsPeriodScoreResponse recordPeriodResult(Long matchId, Integer periodNumber, Integer scoreA, Integer scoreB) {
        log.info("Recording period result matchId={} period={} scoreA={} scoreB={}", matchId, periodNumber, scoreA, scoreB);
        SportsTournamentMatch match = matchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsTournamentMatch", matchId));

        String sportType = resolveSportType(match);
        String label = buildPeriodLabel(sportType, periodNumber);

        List<SportsMatchPeriodScore> existing = periodScoreRepo.findByMatchIdOrderByPeriodNumber(matchId);
        SportsMatchPeriodScore period = existing.stream()
                .filter(p -> p.getPeriodNumber().equals(periodNumber))
                .findFirst()
                .orElse(SportsMatchPeriodScore.builder()
                        .match(match)
                        .periodNumber(periodNumber)
                        .periodLabel(label)
                        .build());

        period.setScoreTeamA(scoreA);
        period.setScoreTeamB(scoreB);
        periodScoreRepo.save(period);

        updateMatchScore(match, sportType);

        return new SportsPeriodScoreResponse(periodNumber, label, scoreA, scoreB);
    }

    @Transactional(readOnly = true)
    public List<SportsPlayerMatchStatsResponse> getPlayerMatchStats(Long matchId) {
        List<SportsPlayerMatchStats> statsList = playerStatsRepo.findByMatchId(matchId);
        return statsList.stream().map(s -> {
            Map<String, Object> statsMap = parseStatsJson(s.getStatsJson());
            return new SportsPlayerMatchStatsResponse(
                    s.getPlayer().getId(),
                    s.getPlayer().getPlayerName(),
                    s.getTeam().getTeamName(),
                    s.getSportType(),
                    statsMap
            );
        }).toList();
    }

    @Transactional
    public void completePeriod(Long matchId, Integer periodNumber) {
        log.info("Completing period matchId={} period={}", matchId, periodNumber);
        SportsTournamentMatch match = matchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsTournamentMatch", matchId));

        String sportType = resolveSportType(match);
        SportsScoringConfig config = resolveConfig(match, sportType);
        ScoringEngine engine = engineRegistry.getEngine(sportType);

        List<SportsMatchPeriodScore> periods = periodScoreRepo.findByMatchIdOrderByPeriodNumber(matchId);

        boolean matchWon = engine.checkMatchWon(match, periods, config);

        if (matchWon) {
            match.setStatus(MatchStatus.COMPLETED);
            match.setCompletedAt(LocalDateTime.now());
            match.setResultType(ResultType.WIN);

            int periodsWonA = 0;
            int periodsWonB = 0;
            for (SportsMatchPeriodScore ps : periods) {
                if (ps.getScoreTeamA() > ps.getScoreTeamB()) periodsWonA++;
                else if (ps.getScoreTeamB() > ps.getScoreTeamA()) periodsWonB++;
            }

            if (config.getPeriodsToWin() != null) {
                if (periodsWonA > periodsWonB) {
                    match.setWinner(match.getTeamA());
                } else if (periodsWonB > periodsWonA) {
                    match.setWinner(match.getTeamB());
                }
            } else {
                int totalA = periods.stream().mapToInt(SportsMatchPeriodScore::getScoreTeamA).sum();
                int totalB = periods.stream().mapToInt(SportsMatchPeriodScore::getScoreTeamB).sum();
                if (totalA > totalB) match.setWinner(match.getTeamA());
                else if (totalB > totalA) match.setWinner(match.getTeamB());
                else match.setResultType(ResultType.DRAW);
            }

            updateMatchScore(match, sportType);
            matchRepo.save(match);
        } else {
            int nextPeriod = periodNumber + 1;
            String label = buildPeriodLabel(sportType, nextPeriod);
            boolean exists = periods.stream().anyMatch(p -> p.getPeriodNumber().equals(nextPeriod));
            if (!exists) {
                SportsMatchPeriodScore next = SportsMatchPeriodScore.builder()
                        .match(match)
                        .periodNumber(nextPeriod)
                        .periodLabel(label)
                        .scoreTeamA(0)
                        .scoreTeamB(0)
                        .build();
                periodScoreRepo.save(next);
            }
        }
    }

    @Transactional
    public SportsScoringConfigResponse saveScoringConfig(SportsScoringConfigRequest req) {
        log.info("Saving scoring config configId={} sportType={}", req.configId(), req.sportType());
        SportsScoringConfig config;
        if (req.configId() != null) {
            config = scoringConfigRepo.findByConfigId(req.configId()).orElse(new SportsScoringConfig());
            SportsTournamentConfig tournamentConfig = new SportsTournamentConfig();
            tournamentConfig.setId(req.configId());
            config.setConfig(tournamentConfig);
        } else {
            config = new SportsScoringConfig();
        }

        config.setSportType(req.sportType());
        config.setPeriodsCount(req.periodsCount() != null ? req.periodsCount() : 2);
        config.setPointsToWinPeriod(req.pointsToWinPeriod());
        config.setMustWinByTwo(req.mustWinByTwo() != null ? req.mustWinByTwo() : false);
        config.setPeriodsToWin(req.periodsToWin());
        config.setPeriodDurationMinutes(req.periodDurationMinutes());
        config.setHasOvertime(req.hasOvertime() != null ? req.hasOvertime() : false);
        config.setHasPenaltyShootout(req.hasPenaltyShootout() != null ? req.hasPenaltyShootout() : false);
        config.setTiebreakPointsToWin(req.tiebreakPointsToWin());
        config.setOversPerInnings(req.oversPerInnings());
        config.setMaxOversPerBowler(req.maxOversPerBowler());
        config.setBallsPerOver(req.ballsPerOver() != null ? req.ballsPerOver() : 6);
        config.setBreakDurationMinutes(req.breakDurationMinutes());
        config.setTargetScore(req.targetScore());
        config.setScoringRulesJson(req.scoringRulesJson());

        scoringConfigRepo.save(config);
        return toConfigResponse(config);
    }

    @Transactional(readOnly = true)
    public SportsScoringConfigResponse getScoringConfig(Long configId) {
        return scoringConfigRepo.findByConfigId(configId)
                .map(this::toConfigResponse)
                .orElse(null);
    }

    public SportsScoringConfigResponse getDefaultScoringConfig(String sportType) {
        return switch (sportType.toUpperCase()) {
            case "CRICKET", "BOX_CRICKET", "GULLY_CRICKET" -> new SportsScoringConfigResponse(
                    null, null, sportType.toUpperCase(), 2, null, false, null, null, false, false, null,
                    20, 4, 6, 15, null, null
            );
            case "BADMINTON" -> new SportsScoringConfigResponse(
                    null, null, "BADMINTON", 3, 21, true, 2, null, false, false, 30,
                    null, null, null, 5, null, null
            );
            case "PICKLEBALL" -> new SportsScoringConfigResponse(
                    null, null, "PICKLEBALL", 3, 11, true, 2, null, false, false, 15,
                    null, null, null, 5, null, null
            );
            case "SQUASH" -> new SportsScoringConfigResponse(
                    null, null, "SQUASH", 5, 11, true, 3, null, false, false, null,
                    null, null, null, 5, null, null
            );
            case "FOOTBALL", "SOCCER" -> new SportsScoringConfigResponse(
                    null, null, sportType.toUpperCase(), 2, null, false, null, 45, true, true, null,
                    null, null, null, 15, null, null
            );
            case "BASKETBALL" -> new SportsScoringConfigResponse(
                    null, null, "BASKETBALL", 4, null, false, null, 12, true, false, null,
                    null, null, null, 15, null, null
            );
            case "VOLLEYBALL", "BEACH_VOLLEYBALL", "GRASS_VOLLEYBALL", "THROWBALL" -> new SportsScoringConfigResponse(
                    null, null, sportType.toUpperCase(), 5, 25, true, 3, null, false, false, 15,
                    null, null, null, 5, null, null
            );
            case "TENNIS" -> new SportsScoringConfigResponse(
                    null, null, "TENNIS", 3, 6, false, 2, null, false, false, 7,
                    null, null, null, 10, null, null
            );
            case "TABLE_TENNIS" -> new SportsScoringConfigResponse(
                    null, null, "TABLE_TENNIS", 5, 11, true, 3, null, false, false, null,
                    null, null, null, 5, null, null
            );
            case "KABADDI" -> new SportsScoringConfigResponse(
                    null, null, "KABADDI", 2, null, false, null, 20, true, false, null,
                    null, null, null, 5, null, null
            );
            case "CARROM" -> new SportsScoringConfigResponse(
                    null, null, "CARROM", 8, 25, false, null, null, false, false, null,
                    null, null, null, 5, 25, null
            );
            case "CHESS" -> new SportsScoringConfigResponse(
                    null, null, "CHESS", 1, null, false, null, 15, false, false, null,
                    null, null, null, null, null, null
            );
            case "FOOSBALL" -> new SportsScoringConfigResponse(
                    null, null, "FOOSBALL", 5, 5, false, 3, null, false, false, null,
                    null, null, null, 5, 5, null
            );
            case "POOL", "BILLIARDS", "SNOOKER" -> new SportsScoringConfigResponse(
                    null, null, sportType.toUpperCase(), 5, null, false, 3, null, false, false, null,
                    null, null, null, 5, null, null
            );
            case "DART" -> new SportsScoringConfigResponse(
                    null, null, "DART", 5, null, false, 3, null, false, false, null,
                    null, null, null, 5, 501, null
            );
            case "BOWLING" -> new SportsScoringConfigResponse(
                    null, null, "BOWLING", 3, null, false, null, null, false, false, null,
                    null, null, null, 5, null, null
            );
            case "TUG_OF_WAR" -> new SportsScoringConfigResponse(
                    null, null, "TUG_OF_WAR", 3, null, false, 2, null, false, false, null,
                    null, null, null, 5, null, null
            );
            default -> new SportsScoringConfigResponse(
                    null, null, sportType.toUpperCase(), 2, null, false, null, null, false, false, null,
                    null, null, null, 10, null, null
            );
        };
    }

    private void updatePeriodScore(SportsTournamentMatch match, Integer periodNumber, Long teamId, int points, String sportType) {
        List<SportsMatchPeriodScore> periods = periodScoreRepo.findByMatchIdOrderByPeriodNumber(match.getId());
        SportsMatchPeriodScore period = periods.stream()
                .filter(p -> p.getPeriodNumber().equals(periodNumber))
                .findFirst()
                .orElse(null);

        if (period == null) {
            String label = buildPeriodLabel(sportType, periodNumber);
            period = SportsMatchPeriodScore.builder()
                    .match(match)
                    .periodNumber(periodNumber)
                    .periodLabel(label)
                    .scoreTeamA(0)
                    .scoreTeamB(0)
                    .build();
        }

        boolean isTeamA = match.getTeamA() != null && match.getTeamA().getId().equals(teamId);
        boolean isTeamB = match.getTeamB() != null && match.getTeamB().getId().equals(teamId);
        if (isTeamA) {
            period.setScoreTeamA(period.getScoreTeamA() + points);
        } else if (isTeamB) {
            period.setScoreTeamB(period.getScoreTeamB() + points);
        }

        periodScoreRepo.save(period);

        SportsScoringConfig config = resolveConfig(match, sportType);
        checkPeriodWon(match, period, config, sportType);
    }

    private void recalculatePeriodScore(SportsTournamentMatch match, Integer periodNumber, String sportType) {
        List<SportsMatchEvent> periodEvents = eventRepo.findByMatchIdAndPeriodNumberAndIsUndoneFalse(match.getId(), periodNumber);

        List<SportsMatchPeriodScore> periods = periodScoreRepo.findByMatchIdOrderByPeriodNumber(match.getId());
        SportsMatchPeriodScore period = periods.stream()
                .filter(p -> p.getPeriodNumber().equals(periodNumber))
                .findFirst()
                .orElse(null);

        if (period == null) return;

        int scoreA = 0;
        int scoreB = 0;
        for (SportsMatchEvent e : periodEvents) {
            boolean isTeamA = match.getTeamA() != null && match.getTeamA().getId().equals(e.getTeam().getId());
            boolean isTeamB = match.getTeamB() != null && match.getTeamB().getId().equals(e.getTeam().getId());
            if (isTeamA) scoreA += e.getPointsAwarded();
            else if (isTeamB) scoreB += e.getPointsAwarded();
        }

        period.setScoreTeamA(scoreA);
        period.setScoreTeamB(scoreB);
        periodScoreRepo.save(period);
    }

    private void checkPeriodWon(SportsTournamentMatch match, SportsMatchPeriodScore period, SportsScoringConfig config, String sportType) {
        ScoringEngine engine = engineRegistry.getEngine(sportType);
        boolean periodWon = engine.checkPeriodWon(match, period, config);

        if (periodWon && config != null && config.getPeriodsToWin() != null) {
            List<SportsMatchPeriodScore> allPeriods = periodScoreRepo.findByMatchIdOrderByPeriodNumber(match.getId());
            if (engine.checkMatchWon(match, allPeriods, config)) {
                match.setStatus(MatchStatus.COMPLETED);
                match.setCompletedAt(LocalDateTime.now());
                match.setResultType(ResultType.WIN);

                int wonA = 0;
                int wonB = 0;
                for (SportsMatchPeriodScore ps : allPeriods) {
                    if (ps.getScoreTeamA() > ps.getScoreTeamB()) wonA++;
                    else if (ps.getScoreTeamB() > ps.getScoreTeamA()) wonB++;
                }
                match.setWinner(wonA > wonB ? match.getTeamA() : match.getTeamB());
                matchRepo.save(match);
            }
        }
    }

    private void updateMatchScore(SportsTournamentMatch match, String sportType) {
        List<SportsMatchPeriodScore> periods = periodScoreRepo.findByMatchIdOrderByPeriodNumber(match.getId());
        ScoringEngine engine = engineRegistry.getEngine(sportType);

        if (POINT_BASED_SPORTS.contains(sportType)) {
            match.setScoreTeamA(engine.formatScoreTeamA(periods));
            match.setScoreTeamB(engine.formatScoreTeamB(periods));
        } else {
            List<SportsMatchEvent> events = eventRepo.findByMatchIdAndIsUndoneFalseOrderByCreatedAt(match.getId());
            int totalA = countPointsForTeam(events, match.getTeamA() != null ? match.getTeamA().getId() : null);
            int totalB = countPointsForTeam(events, match.getTeamB() != null ? match.getTeamB().getId() : null);
            match.setScoreTeamA(String.valueOf(totalA));
            match.setScoreTeamB(String.valueOf(totalB));
        }

        matchRepo.save(match);
    }

    private int countPointsForTeam(List<SportsMatchEvent> events, Long teamId) {
        if (teamId == null) return 0;
        return events.stream()
                .filter(e -> e.getTeam().getId().equals(teamId))
                .mapToInt(SportsMatchEvent::getPointsAwarded)
                .sum();
    }

    private String resolveSportType(SportsTournamentMatch match) {
        if (match.getConfig() != null && match.getConfig().getSport() != null) {
            return match.getConfig().getSport().getName().toUpperCase().replace(" ", "_");
        }
        return scoringConfigRepo.findByConfigId(
                match.getConfig() != null ? match.getConfig().getId() : -1L
        ).map(SportsScoringConfig::getSportType).orElse("UNKNOWN");
    }

    private SportsScoringConfig resolveConfig(SportsTournamentMatch match, String sportType) {
        if (match.getConfig() != null) {
            Optional<SportsScoringConfig> cfg = scoringConfigRepo.findByConfigId(match.getConfig().getId());
            if (cfg.isPresent()) return cfg.get();
        }
        return buildDefaultConfig(sportType);
    }

    private SportsScoringConfig buildDefaultConfig(String sportType) {
        return switch (sportType.toUpperCase()) {
            case "BADMINTON" -> SportsScoringConfig.builder()
                    .sportType("BADMINTON").periodsCount(3).pointsToWinPeriod(21)
                    .mustWinByTwo(true).periodsToWin(2).tiebreakPointsToWin(30).build();
            case "PICKLEBALL" -> SportsScoringConfig.builder()
                    .sportType("PICKLEBALL").periodsCount(3).pointsToWinPeriod(11)
                    .mustWinByTwo(true).periodsToWin(2).tiebreakPointsToWin(15).build();
            case "SQUASH" -> SportsScoringConfig.builder()
                    .sportType("SQUASH").periodsCount(5).pointsToWinPeriod(11)
                    .mustWinByTwo(true).periodsToWin(3).build();
            case "FOOTBALL", "SOCCER" -> SportsScoringConfig.builder()
                    .sportType(sportType.toUpperCase()).periodsCount(2).periodDurationMinutes(45)
                    .hasOvertime(true).hasPenaltyShootout(true).build();
            case "BASKETBALL" -> SportsScoringConfig.builder()
                    .sportType("BASKETBALL").periodsCount(4).periodDurationMinutes(12)
                    .hasOvertime(true).build();
            case "VOLLEYBALL", "BEACH_VOLLEYBALL", "GRASS_VOLLEYBALL", "THROWBALL" -> SportsScoringConfig.builder()
                    .sportType(sportType.toUpperCase()).periodsCount(5).pointsToWinPeriod(25)
                    .mustWinByTwo(true).periodsToWin(3).tiebreakPointsToWin(15).build();
            case "TENNIS" -> SportsScoringConfig.builder()
                    .sportType("TENNIS").periodsCount(3).pointsToWinPeriod(6)
                    .periodsToWin(2).tiebreakPointsToWin(7).build();
            case "TABLE_TENNIS" -> SportsScoringConfig.builder()
                    .sportType("TABLE_TENNIS").periodsCount(5).pointsToWinPeriod(11)
                    .mustWinByTwo(true).periodsToWin(3).build();
            case "KABADDI" -> SportsScoringConfig.builder()
                    .sportType("KABADDI").periodsCount(2).periodDurationMinutes(20)
                    .hasOvertime(true).build();
            case "CARROM" -> SportsScoringConfig.builder()
                    .sportType("CARROM").periodsCount(8).pointsToWinPeriod(25).build();
            case "CHESS" -> SportsScoringConfig.builder()
                    .sportType("CHESS").periodsCount(1).build();
            case "FOOSBALL" -> SportsScoringConfig.builder()
                    .sportType("FOOSBALL").periodsCount(5).pointsToWinPeriod(5).periodsToWin(3).build();
            case "POOL", "BILLIARDS", "SNOOKER" -> SportsScoringConfig.builder()
                    .sportType(sportType.toUpperCase()).periodsCount(5).periodsToWin(3).build();
            case "DART" -> SportsScoringConfig.builder()
                    .sportType("DART").periodsCount(5).periodsToWin(3).build();
            case "BOWLING" -> SportsScoringConfig.builder()
                    .sportType("BOWLING").periodsCount(3).build();
            case "TUG_OF_WAR" -> SportsScoringConfig.builder()
                    .sportType("TUG_OF_WAR").periodsCount(3).periodsToWin(2).build();
            default -> SportsScoringConfig.builder()
                    .sportType(sportType).periodsCount(2).build();
        };
    }

    private String buildPeriodLabel(String sportType, int periodNumber) {
        return switch (sportType.toUpperCase()) {
            case "BADMINTON", "VOLLEYBALL", "BEACH_VOLLEYBALL", "GRASS_VOLLEYBALL",
                 "TENNIS", "TABLE_TENNIS", "PICKLEBALL", "SQUASH", "THROWBALL" -> "Set " + periodNumber;
            case "FOOTBALL", "SOCCER", "KABADDI" -> periodNumber <= 2
                    ? (periodNumber == 1 ? "1st Half" : "2nd Half")
                    : "Extra Time " + (periodNumber - 2);
            case "BASKETBALL" -> "Q" + periodNumber;
            case "CARROM" -> "Board " + periodNumber;
            case "CHESS", "FOOSBALL", "BOWLING" -> "Game " + periodNumber;
            case "POOL", "BILLIARDS", "SNOOKER" -> "Frame " + periodNumber;
            case "DART" -> "Leg " + periodNumber;
            case "TUG_OF_WAR" -> "Round " + periodNumber;
            default -> "Period " + periodNumber;
        };
    }

    private Map<String, Object> parseStatsJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse stats JSON", e);
            return Collections.emptyMap();
        }
    }

    private SportsGenericScoreResponse toResponse(SportsMatchEvent event) {
        SportsAuctionPlayer secondaryPlayer = null;
        if (event.getSecondaryPlayerId() != null) {
            secondaryPlayer = playerRepo.findById(event.getSecondaryPlayerId()).orElse(null);
        }

        return new SportsGenericScoreResponse(
                event.getId(),
                event.getMatch().getId(),
                event.getTeam().getId(),
                event.getTeam().getTeamName(),
                event.getPlayer() != null ? event.getPlayer().getId() : null,
                event.getPlayer() != null ? event.getPlayer().getPlayerName() : null,
                event.getEventType(),
                event.getPeriodNumber(),
                event.getMatchMinute(),
                event.getPointsAwarded(),
                event.getDescription(),
                event.getSecondaryPlayerId(),
                secondaryPlayer != null ? secondaryPlayer.getPlayerName() : null,
                event.getCreatedAt() != null ? event.getCreatedAt().toString() : null
        );
    }

    private SportsScoringConfigResponse toConfigResponse(SportsScoringConfig config) {
        return new SportsScoringConfigResponse(
                config.getId(),
                config.getConfig() != null ? config.getConfig().getId() : null,
                config.getSportType(),
                config.getPeriodsCount(),
                config.getPointsToWinPeriod(),
                config.getMustWinByTwo(),
                config.getPeriodsToWin(),
                config.getPeriodDurationMinutes(),
                config.getHasOvertime(),
                config.getHasPenaltyShootout(),
                config.getTiebreakPointsToWin(),
                config.getOversPerInnings(),
                config.getMaxOversPerBowler(),
                config.getBallsPerOver(),
                config.getBreakDurationMinutes(),
                config.getTargetScore(),
                config.getScoringRulesJson()
        );
    }

    @Transactional
    public SportsTournamentMatch updateMatchConfigOverride(Long matchId, String overrideJson) {
        log.info("Updating match config override matchId={}", matchId);
        SportsTournamentMatch match = matchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsTournamentMatch", matchId));
        match.setMatchConfigOverrideJson(overrideJson);
        return matchRepo.save(match);
    }
}
