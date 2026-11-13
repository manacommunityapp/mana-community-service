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
    private final SportsCourtRepository             courtRepo;
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
        return rescheduleMatch(matchId, scheduledAtStr, venue, null);
    }

    public SportsTournamentMatch rescheduleMatch(Long matchId, String scheduledAtStr, String venue, String court) {
        SportsTournamentMatch match = matchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsTournamentMatch", matchId));
        LocalDateTime scheduledAt;
        try {
            if (scheduledAtStr.endsWith("Z")) {
                scheduledAt = java.time.Instant.parse(scheduledAtStr).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            } else if (scheduledAtStr.contains("+") || (scheduledAtStr.lastIndexOf('-') > 10)) {
                scheduledAt = java.time.OffsetDateTime.parse(scheduledAtStr).toLocalDateTime();
            } else {
                scheduledAt = LocalDateTime.parse(scheduledAtStr);
            }
        } catch (Exception e) {
            log.debug("Fallback parsing for scheduledAt '{}': {}", scheduledAtStr, e.getMessage());
            scheduledAt = LocalDateTime.parse(scheduledAtStr);
        }
        match.setScheduledAt(scheduledAt);
        if (venue != null && !venue.isBlank()) {
            try {
                Long venueId = Long.parseLong(venue);
                match.setVenue(venueRepo.findById(venueId).orElseThrow(() -> new ResourceNotFoundException("Venue", venueId)));
            } catch (NumberFormatException e) {
                log.debug("Non-numeric venue value '{}', treating as legacy string venue", venue);
            }
        }
        if (court != null && !court.isBlank()) {
            try {
                Long courtId = Long.parseLong(court);
                match.setCourt(courtRepo.findById(courtId).orElse(null));
            } catch (NumberFormatException e) {
                log.debug("Non-numeric court value '{}', ignored", court);
            }
        }
        return matchRepo.save(match);
    }

    @Transactional
    public SportsBatchRescheduleResponse batchReschedule(Long configId, SportsBatchRescheduleRequest req) {
        Long targetConfigId = configId != null ? configId : req.configId();
        List<SportsTournamentMatch> matches;
        if (req.matchIds() != null && !req.matchIds().isEmpty()) {
            matches = matchRepo.findAllById(req.matchIds());
        } else if (targetConfigId != null) {
            matches = matchRepo.findByConfigIdOrderByScheduledAt(targetConfigId);
        } else {
            return new SportsBatchRescheduleResponse(0, "No tournament or matches specified", List.of());
        }

        // Filter: NEVER touch completed, cancelled or bye matches
        LocalDateTime cutoff = null;
        if (req.fromDateTime() != null && !req.fromDateTime().isBlank()) {
            try {
                cutoff = parseDateTime(req.fromDateTime());
            } catch (Exception e) {
                log.warn("Failed to parse fromDateTime '{}': {}", req.fromDateTime(), e.getMessage());
            }
        }
        final LocalDateTime finalCutoff = cutoff;

        List<SportsTournamentMatch> eligible = matches.stream()
                .filter(m -> m.getStatus() != MatchStatus.COMPLETED && m.getStatus() != MatchStatus.CANCELLED)
                .filter(m -> m.getStatus() != MatchStatus.BYE && m.getStatus() != MatchStatus.AUTO_ADVANCED)
                .filter(m -> finalCutoff == null || (m.getScheduledAt() != null && !m.getScheduledAt().isBefore(finalCutoff)))
                .sorted(java.util.Comparator.comparing(SportsTournamentMatch::getScheduledAt, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                        .thenComparing(m -> Objects.requireNonNullElse(m.getMatchNumber(), 0)))
                .toList();

        if (eligible.isEmpty()) {
            return new SportsBatchRescheduleResponse(0, "No eligible uncompleted matches found to reschedule", List.of());
        }

        String mode = req.mode() != null ? req.mode().trim().toUpperCase() : "TIME_SHIFT";

        if ("TIME_SHIFT".equals(mode)) {
            int delayMinutes = req.delayMinutes() != null ? req.delayMinutes() : 0;
            for (SportsTournamentMatch m : eligible) {
                if (m.getScheduledAt() != null) {
                    m.setScheduledAt(m.getScheduledAt().plusMinutes(delayMinutes));
                }
            }
        } else if ("NEW_DATE".equals(mode)) {
            if (req.newDate() == null || req.newDate().isBlank()) {
                throw new IllegalArgumentException("newDate must be provided for NEW_DATE mode");
            }
            java.time.LocalDate targetDate = java.time.LocalDate.parse(req.newDate().trim());
            java.time.LocalTime targetStartTime = parseLocalTime(req.startTime());

            if (req.intervalMinutes() != null && req.intervalMinutes() > 0) {
                // Group by Court (or null for unassigned) to prevent court double-booking
                Map<Long, List<SportsTournamentMatch>> byCourt = new java.util.LinkedHashMap<>();
                for (SportsTournamentMatch m : eligible) {
                    Long cId = m.getCourt() != null ? m.getCourt().getId() : -1L;
                    byCourt.computeIfAbsent(cId, k -> new java.util.ArrayList<>()).add(m);
                }
                for (List<SportsTournamentMatch> courtMatches : byCourt.values()) {
                    for (int i = 0; i < courtMatches.size(); i++) {
                        LocalDateTime newDt = LocalDateTime.of(targetDate, targetStartTime).plusMinutes((long) i * req.intervalMinutes());
                        courtMatches.get(i).setScheduledAt(newDt);
                    }
                }
            } else {
                // Preserve relative offsets between matches from the first match
                LocalDateTime baseOriginal = eligible.stream()
                        .map(SportsTournamentMatch::getScheduledAt)
                        .filter(Objects::nonNull)
                        .min(java.util.Comparator.naturalOrder())
                        .orElse(LocalDateTime.now());

                LocalDateTime targetBase = LocalDateTime.of(targetDate, targetStartTime);
                for (SportsTournamentMatch m : eligible) {
                    if (m.getScheduledAt() != null) {
                        long diff = java.time.Duration.between(baseOriginal, m.getScheduledAt()).toMinutes();
                        m.setScheduledAt(targetBase.plusMinutes(Math.max(0, diff)));
                    } else {
                        m.setScheduledAt(targetBase);
                    }
                }
            }
        }

        List<SportsTournamentMatch> saved = matchRepo.saveAll(eligible);
        log.info("Batch rescheduled {} matches for configId={} in mode={}", saved.size(), targetConfigId, mode);
        return new SportsBatchRescheduleResponse(
                saved.size(),
                "Successfully rescheduled " + saved.size() + " matches",
                saved.stream().map(this::toMatchResponse).toList()
        );
    }

    private LocalDateTime parseDateTime(String dtStr) {
        if (dtStr == null || dtStr.isBlank()) return null;
        if (dtStr.endsWith("Z")) {
            return java.time.Instant.parse(dtStr).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        } else if (dtStr.contains("+") || (dtStr.lastIndexOf('-') > 10)) {
            return java.time.OffsetDateTime.parse(dtStr).toLocalDateTime();
        }
        return LocalDateTime.parse(dtStr);
    }

    private java.time.LocalTime parseLocalTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) {
            return java.time.LocalTime.of(9, 0);
        }
        String t = timeStr.trim().toUpperCase();
        var m12 = java.util.regex.Pattern.compile("^(\\d{1,2}):(\\d{2})\\s*(AM|PM)?$").matcher(t);
        if (m12.find()) {
            int h = Integer.parseInt(m12.group(1));
            int m = Integer.parseInt(m12.group(2));
            String meridiem = m12.group(3);
            if ("PM".equalsIgnoreCase(meridiem) && h < 12) h += 12;
            if ("AM".equalsIgnoreCase(meridiem) && h == 12) h = 0;
            return java.time.LocalTime.of(h, m);
        }
        var m24 = java.util.regex.Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(t);
        if (m24.find()) {
            return java.time.LocalTime.of(Integer.parseInt(m24.group(1)), Integer.parseInt(m24.group(2)));
        }
        return java.time.LocalTime.of(9, 0);
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

    public void deleteTournamentConfig(Long configId) {
        matchPersistence.deleteTournamentConfig(configId);
    }

    @Transactional
    public SportsTournamentConfigResponse publishTournamentConfig(Long configId) {
        SportsTournamentConfig config = configRepo.findById(configId)
            .orElseThrow(() -> new ResourceNotFoundException("SportsTournamentConfig", configId));

        config.setStatus(SportsTournamentConfig.TournamentStatus.ACTIVE);
        SportsTournamentConfig saved = configRepo.save(config);

        matchRepo.updateStatusByConfigId(saved.getId(), MatchStatus.PUBLISHED);
        notificationService.notifySchedulePublished(saved.getId());

        log.info("Published tournament config id={} name={}", saved.getId(), saved.getTournamentName());
        return toConfigResponse(saved);
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
        String teamAName = m.getTeamA() != null ? m.getTeamA().getTeamName() : (m.getPlaceholderTeamA() != null && !m.getPlaceholderTeamA().isBlank() ? m.getPlaceholderTeamA() : extractNote(m.getMatchNotes(), "homeName", "TBD"));
        String teamBName = m.getTeamB() != null ? m.getTeamB().getTeamName() : (m.getPlaceholderTeamB() != null && !m.getPlaceholderTeamB().isBlank() ? m.getPlaceholderTeamB() : extractNote(m.getMatchNotes(), "awayName", "TBD"));
        String roundName = m.getRound() != null ? m.getRound().name() : extractNote(m.getMatchNotes(), "groupName", "TBD");
        String teamAFlat = extractNote(m.getMatchNotes(), "homeFlat", null);
        String teamBFlat = extractNote(m.getMatchNotes(), "awayFlat", null);

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
            m.getStatus() == MatchStatus.BYE || m.getStatus() == MatchStatus.AUTO_ADVANCED,
            teamAFlat,
            teamBFlat,
            m.getPlaceholderTeamA(),
            m.getPlaceholderTeamB()
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
