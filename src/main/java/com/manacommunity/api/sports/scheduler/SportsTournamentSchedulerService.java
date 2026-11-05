package com.manacommunity.api.sports.scheduler;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.dto.GroupAssignmentRequest;
import com.manacommunity.api.sports.dto.SportsTournamentGroupResponse;
import com.manacommunity.api.sports.dto.RoundResponse;
import com.manacommunity.api.sports.dto.StandingResponse;
import com.manacommunity.api.sports.model.MatchStatus;
import com.manacommunity.api.sports.model.SportsTournament;
import com.manacommunity.api.sports.model.SportsVenue;
import com.manacommunity.api.sports.repository.SportsVenueRepository;


import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.sports.model.SportsAuctionTeam;
import com.manacommunity.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Orchestrator/facade for tournament scheduling. Holds the focused collaborators
 * (validation, seeding, bracket generation, court/time allocation, persistence,
 * notifications) and exposes the public API the controller depends on, delegating
 * the work. Response mapping and simple reads live here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SportsTournamentSchedulerService {

    // ── Collaborators (the segregated services) ───────────────────────
    private final SportsRegistrationValidator       registrationValidator;
    private final SportsSeedingService              seedingService;
    private final SportsBracketGenerator            bracketGenerator;
    private final SportsMatchPersistenceService     matchPersistence;
    private final SportsSchedulerNotificationService notificationService;
    private final SportsTournamentResultNotifier     resultNotifier;

    // ── Repositories used for read/response mapping only ──────────────
    private final SportsTournamentConfigRepository  configRepo;
    private final SportsTournamentGroupRepository   groupRepo;
    private final SportsTournamentMatchRepository   matchRepo;
    private final SportsGroupTeamStandingRepository standingRepo;
    private final SportsVenueRepository             venueRepo;
    private final SportsAuctionTeamRepository       teamRepo;
    private final com.manacommunity.api.sports.repository.SportsAuctionConfigRepository auctionConfigRepo;
    private final SportsEventRepository             eventRepo;

    public List<Map<String, Object>> getConfigTeams(Long configId) {
        if (configId == null) {
            return java.util.Collections.emptyList();
        }
        SportsTournamentConfig config = configRepo.findById(configId)
                .or(() -> configRepo.findByEventId(configId).stream().findFirst())
                .orElse(null);

        List<SportsAuctionTeam> teams = java.util.Collections.emptyList();
        if (config != null) {
            teams = teamRepo.findByConfigIdOrderByTeamName(config.getId());
        }

        if (teams.isEmpty()) {
            Long eventId = (config != null && config.getEvent() != null) ? config.getEvent().getId() : configId;
            teams = teamRepo.findByEventIdOrderByTeamName(eventId);
            if (teams.isEmpty()) {
                var ac = auctionConfigRepo.findByEventId(eventId).orElse(null);
                if (ac != null) {
                    teams = teamRepo.findByConfigIdOrderByTeamName(ac.getId());
                }
            }
        }

        java.util.Map<Long, String> teamGroupMap = new java.util.HashMap<>();
        if (config != null) {
            List<SportsTournamentGroup> groups = groupRepo.findByConfigIdOrderByGroupOrder(config.getId());
            for (SportsTournamentGroup g : groups) {
                List<SportsGroupTeamStanding> standings = standingRepo.findByGroupIdOrderByPointsDescNetRunRateDesc(g.getId());
                for (SportsGroupTeamStanding s : standings) {
                    if (s.getTeam() != null) {
                        teamGroupMap.put(s.getTeam().getId(), g.getGroupName());
                    }
                }
            }
        }

        return teams.stream().map(t -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", t.getId());
            map.put("teamId", t.getId());
            map.put("name", t.getTeamName());
            map.put("teamName", t.getTeamName());
            map.put("color", t.getColorHex() != null ? t.getColorHex() : "#3b82f6");
            map.put("colorHex", t.getColorHex() != null ? t.getColorHex() : "#3b82f6");
            map.put("group", teamGroupMap.getOrDefault(t.getId(), "UNASSIGNED"));
            map.put("ownerName", t.getOwnerName() != null ? t.getOwnerName() : "");
            return map;
        }).toList();
    }

    // ═══════════════════════════════════════════════════════════════
    // ENTRY POINT — builds config + schedule from request
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public SportsTournamentScheduleResponse createTournamentSchedule(
            SportsTournamentConfigRequest req, Long adminUserId) {

        // Validate teams
        List<SportsAuctionTeam> teams = registrationValidator.validateTeams(req);

        // Persist config (DRAFT)
        SportsTournamentConfig config = matchPersistence.saveTournamentConfig(req, adminUserId);
        log.info("SportsTournament config saved: [{}] type={}", config.getTournamentName(), config.getTournamentType());

        // Seed if required
        if (Boolean.TRUE.equals(req.hasSeeding())) {
            teams = seedingService.seed(teams, req.sportId(), req.communityId(), "CURRENT");
        }

        // Generate + persist schedule
        List<SportsTournamentMatch> matches = bracketGenerator.generate(config, teams);
        List<SportsTournamentMatch> savedMatches = matchRepo.saveAll(matches);
        matchPersistence.wireAndSaveKnockoutLinks(savedMatches);
        log.info("Generated {} matches for tournament [{}]", savedMatches.size(), config.getTournamentName());
        return buildResponse(config);
    }

    public SportsTournamentScheduleResponse getSchedule(Long configId) {
        if (configId == null) {
            return new SportsTournamentScheduleResponse(0L, "Tournament", "ROUND_ROBIN", 0, 0, 0, java.time.LocalDate.now(), java.time.LocalDate.now(), List.of(), List.of(), List.of());
        }
        SportsTournamentConfig config = configRepo.findById(configId)
                .or(() -> configRepo.findByEventId(configId).stream().findFirst())
                .orElse(null);

        if (config == null) {
            var eventOpt = eventRepo.findById(configId);
            if (eventOpt.isPresent()) {
                var event = eventOpt.get();
                return new SportsTournamentScheduleResponse(
                    configId,
                    event.getName() != null ? event.getName() : "Event #" + configId,
                    "ROUND_ROBIN",
                    0, 0, 0,
                    event.getEventDateStart() != null ? event.getEventDateStart() : java.time.LocalDate.now(),
                    event.getEventDateEnd() != null ? event.getEventDateEnd() : java.time.LocalDate.now(),
                    List.of(),
                    List.of(),
                    List.of()
                );
            }
            return new SportsTournamentScheduleResponse(
                configId,
                "Tournament #" + configId,
                "ROUND_ROBIN",
                0, 0, 0,
                java.time.LocalDate.now(),
                java.time.LocalDate.now(),
                List.of(),
                List.of(),
                List.of()
            );
        }
        return buildResponse(config);
    }

    public SportsTournamentMatch rescheduleMatch(Long matchId, String scheduledAtStr, String venue) {
        SportsTournamentMatch match = matchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsTournamentMatch", matchId));
        LocalDateTime scheduledAt = LocalDateTime.parse(scheduledAtStr);
        match.setScheduledAt(scheduledAt);
        if (venue != null) {
            try {
                Long venueId = Long.parseLong(venue);
                match.setVenue(venueRepo.findById(venueId).orElseThrow(() -> new ResourceNotFoundException("Venue", venueId)));
            } catch (NumberFormatException e) {
                log.debug("Non-numeric venue value '{}', treating as legacy string venue", venue);
            }
        }
        return matchRepo.save(match);
    }

    public List<SportsMatchResponse> getUpcomingMatchesForTeam(Long teamId) {
        // Placeholder — returns empty list (no custom query yet).
        return List.of();
    }

    // ═══════════════════════════════════════════════════════════════
    // BRACKET advance / seeding / swiss (delegate to SportsBracketGenerator)
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public SportsTournamentScheduleResponse advanceBracket(SportsMatchResultRequest req) {
        SportsTournamentConfig config = bracketGenerator.applyResult(req);
        // Winner notification (every result) + tournament-completion email (on the FINAL).
        resultNotifier.notifyMatchResult(req.matchId());
        return buildResponse(config);
    }

    public void seedKnockoutFromGroups(Long configId) {
        bracketGenerator.seedKnockoutFromGroups(configId);
    }

    public List<SportsTournamentMatch> generateNextSwissRound(Long configId) {
        return bracketGenerator.generateNextSwissRound(configId);
    }

    // ═══════════════════════════════════════════════════════════════
    // CONFIG CRUD / MANUAL / PERSISTENCE (delegate to SportsMatchPersistenceService)
    // ═══════════════════════════════════════════════════════════════
    public SportsTournamentConfig saveTournamentConfig(SportsTournamentConfigRequest req, Long adminUserId) {
        return matchPersistence.saveTournamentConfig(req, adminUserId);
    }

    public SportsTournamentConfig updateTournamentConfig(Long configId, SportsTournamentConfigRequest req) {
        return matchPersistence.updateTournamentConfig(configId, req);
    }

    public SportsTournamentConfigResponse toConfigResponse(SportsTournamentConfig c) {
        return matchPersistence.toConfigResponse(c);
    }

    public List<SportsTournamentConfigResponse> getConfigsByCommunity(Long communityId) {
        return matchPersistence.getConfigsByCommunity(communityId);
    }

    public void assignTeamsToGroups(Long configId, List<GroupAssignmentRequest> assignments) {
        matchPersistence.assignTeamsToGroups(configId, assignments);
    }

    public SportsTournamentMatch scheduleManualMatch(Long configId, SportsMatchScheduleRequest req) {
        return matchPersistence.scheduleManualMatch(configId, req);
    }

    public void deleteMatch(Long matchId) {
        matchPersistence.deleteMatch(matchId);
    }

    /**
     * Unified deferred save. Persistence runs in its own transaction; on PUBLISHED
     * we notify participants AFTER it commits so a rollback never sends notifications.
     */
    public SportsScheduleSaveResponse saveSchedule(SportsScheduleSaveRequest req, Long adminUserId) {
        SportsScheduleSaveResponse resp = matchPersistence.saveSchedule(req, adminUserId);
        if (isPublished(req.status()) && resp.config() != null) {
            notificationService.notifySchedulePublished(resp.config().id());
        }
        return resp;
    }

    public int saveMatchesBulk(Long configId, List<SportsBulkMatchSaveRequest.MatchData> matches) {
        return matchPersistence.saveMatchesBulk(configId, matches);
    }

    public int updateMatchesStatus(Long configId, String statusStr) {
        int updated = matchPersistence.updateMatchesStatus(configId, statusStr);
        if (isPublished(statusStr)) {
            notificationService.notifySchedulePublished(configId);
        }
        return updated;
    }

    public int deleteMatchesByConfigId(Long configId) {
        return matchPersistence.deleteMatchesByConfigId(configId);
    }

    private boolean isPublished(String status) {
        return status != null && "PUBLISHED".equalsIgnoreCase(status.trim());
    }

    // ═══════════════════════════════════════════════════════════════
    // RESPONSE MAPPING (reads)
    // ═══════════════════════════════════════════════════════════════
    private SportsTournamentScheduleResponse buildResponse(SportsTournamentConfig config) {
        List<SportsTournamentMatch> matches = matchRepo.findByConfigIdOrderByScheduledAt(config.getId());
        List<SportsTournamentGroup> groups  = groupRepo.findByConfigIdOrderByGroupOrder(config.getId());

        int totalRounds = matches.stream()
            .mapToInt(m -> Objects.requireNonNullElse(m.getRoundNumber(), 1))
            .max().orElse(0);

        // Build group responses
        List<SportsTournamentGroupResponse> groupResponses = groups.stream().map(g -> {
            List<StandingResponse> standings = standingRepo
                .findByGroupIdOrderByPointsDescNetRunRateDesc(g.getId())
                .stream().map(this::toStandingResponse)
                .collect(Collectors.toList());
            List<SportsMatchResponse> gMatches = matches.stream()
                .filter(m -> g.equals(m.getGroup()))
                .map(this::toMatchResponse).toList();
            return new SportsTournamentGroupResponse(g.getId(), g.getGroupName(), standings, gMatches);
        }).toList();

        // Build round responses (null-safe roundNumber grouping and sorting)
        Map<Integer, List<SportsTournamentMatch>> byRound = matches.stream()
            .collect(Collectors.groupingBy(m -> Objects.requireNonNullElse(m.getRoundNumber(), 1)));
        List<RoundResponse> rounds = byRound.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> new RoundResponse(
                (e.getValue().get(0).getRound() != null ? e.getValue().get(0).getRound().name() : "ROUND " + e.getKey()),
                e.getKey(),
                e.getValue().stream().map(this::toMatchResponse).toList()))
            .toList();

        return new SportsTournamentScheduleResponse(
            config.getId(), config.getTournamentName(), config.getTournamentType().name(),
            config.getTotalTeams(), matches.size(), totalRounds,
            config.getStartDate(), config.getEndDate(),
            groupResponses, rounds,
            matches.stream().map(this::toMatchResponse).toList()
        );
    }

    public SportsMatchResponse toMatchResponse(SportsTournamentMatch m) {
        String teamAName = m.getTeamA() != null ? m.getTeamA().getTeamName() : extractNote(m.getMatchNotes(), "homeName", "TBD");
        String teamBName = m.getTeamB() != null ? m.getTeamB().getTeamName() : extractNote(m.getMatchNotes(), "awayName", "TBD");
        String roundName = m.getRound() != null ? m.getRound().name() : extractNote(m.getMatchNotes(), "groupName", "TBD");

        return new SportsMatchResponse(
            m.getId(), roundName,
            Objects.requireNonNullElse(m.getMatchNumber(), 0),
            Objects.requireNonNullElse(m.getBracketSlot(), 0),
            m.getTeamA() != null ? m.getTeamA().getId() : null,
            teamAName,
            m.getTeamB() != null ? m.getTeamB().getId() : null,
            teamBName,
            m.getTeamA() != null ? m.getTeamA().getColorHex() : "#555",
            m.getTeamB() != null ? m.getTeamB().getColorHex() : "#555",
            m.getScheduledAt() != null ? m.getScheduledAt().toString() : "",
            m.getVenue() != null ? m.getVenue().getId() : null,
            m.getVenue() != null ? m.getVenue().getName() : null,
            m.getCourt() != null ? m.getCourt().getId() : null,
            m.getCourt() != null ? m.getCourt().getName() : null,
            m.getStatus() != null ? m.getStatus().name() : "SCHEDULED",
            m.getScoreTeamA(), m.getScoreTeamB(),
            m.getWinner() != null ? m.getWinner().getTeamName() : (m.getWinnerName() != null ? m.getWinnerName() : null),
            m.getWinnerAdvancesToMatchId(),
            m.getStatus() == MatchStatus.BYE || m.getStatus() == MatchStatus.AUTO_ADVANCED
        );
    }

    private String extractNote(String notes, String key, String fallback) {
        if (notes == null || notes.isBlank()) return fallback;
        try {
            String pattern = "\"" + key + "\":\"([^\"]*)\"";
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(notes);
            if (matcher.find()) {
                String val = matcher.group(1);
                if (val != null && !val.isBlank() && !"dummy".equalsIgnoreCase(val)) {
                    return val;
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract {} from matchNotes: {}", key, e.getMessage());
        }
        return fallback;
    }


    private StandingResponse toStandingResponse(SportsGroupTeamStanding s) {
        return new StandingResponse(
            0,
            s.getTeam() != null ? s.getTeam().getId() : 0L,
            s.getTeam() != null ? s.getTeam().getTeamName() : "TBD",
            s.getTeam() != null ? s.getTeam().getColorHex() : "#555",
            s.getPlayed(), s.getWon(), s.getLost(), s.getDrawn(), s.getPoints(),
            Objects.requireNonNullElse(s.getNetRunRate(), 0.0), s.getQualified()
        );
    }
}
