package com.manacommunity.api.service.scheduler;

import com.manacommunity.api.dto.scheduler.*;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.SportsAuctionTeam;
import com.manacommunity.api.model.scheduler.*;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.repository.scheduler.*;
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
    private final VenueRepository             venueRepo;

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
            teams = seedingService.seed(teams);
        }

        // Generate + persist schedule
        List<SportsTournamentMatch> matches = bracketGenerator.generate(config, teams);
        matchRepo.saveAll(matches);
        log.info("Generated {} matches for tournament [{}]", matches.size(), config.getTournamentName());
        return buildResponse(config);
    }

    public SportsTournamentScheduleResponse getSchedule(Long configId) {
        SportsTournamentConfig config = configRepo.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsTournamentConfig", configId));
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
                // legacy string venue — ignore
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

    public void scheduleManualMatch(Long configId, SportsMatchScheduleRequest req) {
        matchPersistence.scheduleManualMatch(configId, req);
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
            .mapToInt(SportsTournamentMatch::getRoundNumber).max().orElse(0);

        // Build group responses
        List<GroupResponse> groupResponses = groups.stream().map(g -> {
            List<StandingResponse> standings = standingRepo
                .findByGroupIdOrderByPointsDescNetRunRateDesc(g.getId())
                .stream().map(this::toStandingResponse)
                .collect(Collectors.toList());
            List<SportsMatchResponse> gMatches = matches.stream()
                .filter(m -> g.equals(m.getGroup()))
                .map(this::toMatchResponse).toList();
            return new GroupResponse(g.getId(), g.getGroupName(), standings, gMatches);
        }).toList();

        // Build round responses
        Map<Integer, List<SportsTournamentMatch>> byRound = matches.stream()
            .collect(Collectors.groupingBy(SportsTournamentMatch::getRoundNumber));
        List<RoundResponse> rounds = byRound.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> new RoundResponse(
                e.getValue().get(0).getRound().name(),
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
        return new SportsMatchResponse(
            m.getId(), m.getRound() != null ? m.getRound().name() : "TBD",
            Objects.requireNonNullElse(m.getMatchNumber(), 0),
            Objects.requireNonNullElse(m.getBracketSlot(), 0),
            m.getTeamA() != null ? m.getTeamA().getId() : null,
            m.getTeamA() != null ? m.getTeamA().getTeamName() : "TBD",
            m.getTeamB() != null ? m.getTeamB().getId() : null,
            m.getTeamB() != null ? m.getTeamB().getTeamName() : "TBD",
            m.getTeamA() != null ? m.getTeamA().getColorHex() : "#555",
            m.getTeamB() != null ? m.getTeamB().getColorHex() : "#555",
            m.getScheduledAt() != null ? m.getScheduledAt().toString() : "",
            m.getVenue() != null ? m.getVenue().getId() : null,
            m.getVenue() != null ? m.getVenue().getName() : null,
            m.getCourt() != null ? m.getCourt().getId() : null,
            m.getCourt() != null ? m.getCourt().getName() : null,
            m.getStatus().name(),
            m.getScoreTeamA(), m.getScoreTeamB(),
            m.getWinner() != null ? m.getWinner().getTeamName() : null,
            m.getWinnerAdvancesToMatchId(),
            m.getStatus() == MatchStatus.BYE
        );
    }

    private StandingResponse toStandingResponse(SportsGroupTeamStanding s) {
        return new StandingResponse(
            0, s.getTeam().getId(), s.getTeam().getTeamName(),
            s.getTeam().getColorHex(),
            s.getPlayed(), s.getWon(), s.getLost(), s.getDrawn(), s.getPoints(),
            Objects.requireNonNullElse(s.getNetRunRate(), 0.0), s.getQualified()
        );
    }
}
