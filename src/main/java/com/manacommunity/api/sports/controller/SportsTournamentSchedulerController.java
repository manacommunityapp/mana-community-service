package com.manacommunity.api.sports.controller;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.sports.dto.GroupAssignmentRequest;
import com.manacommunity.api.sports.model.SportsAuctionConfig;
import com.manacommunity.api.sports.repository.SportsAuctionConfigRepository;


import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.sports.model.SportsEvent;
import com.manacommunity.api.sports.scheduler.SportsTournamentConfig;
import com.manacommunity.api.sports.scheduler.SportsTournamentMatch;
import com.manacommunity.api.sports.repository.SportsEventRepository;
import com.manacommunity.api.sports.repository.SportsTournamentConfigRepository;
import com.manacommunity.api.sports.repository.SportsTournamentMatchRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.sports.scheduler.SportsTournamentSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.manacommunity.api.constants.PermissionConstants.ROLE_SUPER_ADMIN;

@Slf4j
@RestController
@RequestMapping("/api/tournament")
@RequiredArgsConstructor
public class SportsTournamentSchedulerController {

    private final SportsTournamentSchedulerService schedulerService;
    private final SportsTournamentConfigRepository configRepo;
    private final SportsTournamentMatchRepository  matchRepo;
    private final SportsEventRepository      eventRepo;
    private final LoggedInUserService        loggedInUserService;
    private final com.manacommunity.api.sports.repository.SportsAuctionConfigRepository auctionConfigRepo;
    private final SportsTournamentRepository tournamentRepo;
    private final com.manacommunity.api.sports.scheduler.SportsPlayoffScheduleGenerator playoffGenerator;
    private final com.manacommunity.api.sports.scheduler.SportsMatchResultService matchResultService;

    // ═══════════════════════════════════════════════════════════════
    // CONFIG CRUD — save / update (without schedule generation)
    // ═══════════════════════════════════════════════════════════════

    /**
     * POST /api/tournament/config
     * Save a new tournament config in DRAFT status.
     */
    @PostMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<SportsTournamentConfigResponse> saveConfig(
            @Valid @RequestBody SportsTournamentConfigRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Creating tournament config userId={}", principal.getId());
        SportsTournamentConfig saved = schedulerService.saveTournamentConfig(req, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(schedulerService.toConfigResponse(saved));
    }

    /**
     * PUT /api/tournament/config/{id}
     * Update an existing tournament config.
     */
    @PutMapping("/config/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<SportsTournamentConfigResponse> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody SportsTournamentConfigRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Updating tournament config id={}", id);
        SportsTournamentConfig updated = schedulerService.updateTournamentConfig(id, req);
        return ResponseEntity.ok(schedulerService.toConfigResponse(updated));
    }

    /**
     * GET /api/tournament/config/{id}
     * Get a single tournament config by ID.
     */
    @GetMapping("/config/{id}")
    public ResponseEntity<SportsTournamentConfigResponse> getConfig(@PathVariable Long id) {
        SportsTournamentConfig config = configRepo.findById(id)
                .or(() -> configRepo.findByEventId(id).stream().findFirst())
                .orElse(null);
        if (config == null) {
            SportsEvent event = eventRepo.findById(id).orElse(null);
            if (event != null) {
                SportsTournamentConfig draft = SportsTournamentConfig.builder()
                    .id(event.getId())
                    .tournamentName(event.getName() != null ? event.getName() : "Event #" + id)
                    .sport(event.getSport())
                    .community(event.getCommunity())
                    .event(event)
                    .tournamentType(com.manacommunity.api.sports.model.TournamentType.ROUND_ROBIN)
                    .totalTeams(8)
                    .numberOfGroups(2)
                    .startDate(event.getEventDateStart() != null ? event.getEventDateStart() : java.time.LocalDate.now())
                    .endDate(event.getEventDateEnd() != null ? event.getEventDateEnd() : java.time.LocalDate.now())
                    .status(SportsTournamentConfig.TournamentStatus.DRAFT)
                    .matchDurationMinutes(60)
                    .pointsForWin(2)
                    .pointsForDraw(1)
                    .pointsForLoss(0)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
                return ResponseEntity.ok(schedulerService.toConfigResponse(draft));
            }
            throw new com.manacommunity.api.exception.ResourceNotFoundException("SportsTournamentConfig", id);
        }
        return ResponseEntity.ok(schedulerService.toConfigResponse(config));
    }

    /**
     * GET /api/tournament/configs
     * Get all tournament configs for the logged-in user's community.
     */
    @GetMapping("/configs")
    public ResponseEntity<List<SportsTournamentConfigResponse>> getConfigs(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(schedulerService.getConfigsByCommunity(communityId));
    }

    /**
     * GET /api/tournament/{configId}/teams
     * Returns all participating teams for a tournament config.
     */
    @GetMapping("/{configId}/teams")
    public ResponseEntity<List<Map<String, Object>>> getConfigTeams(@PathVariable Long configId) {
        log.info("Fetching teams for tournament configId={}", configId);
        return ResponseEntity.ok(schedulerService.getConfigTeams(configId));
    }

    // ═══════════════════════════════════════════════════════════════
    // DROPDOWN DATA — events, types, teams
    // ═══════════════════════════════════════════════════════════════

    /**
     * GET /api/tournament/events
     * Returns sports events for the logged-in user's community
     * to populate the "Event" dropdown in the tournament config form.
     */
    @GetMapping("/events")
    public ResponseEntity<List<Map<String, Object>>> getEventsForDropdown(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;

        List<SportsEvent> events;
        if (communityId != null) {
            events = eventRepo.findByCommunityIdOrderByEventDateStartDesc(communityId);
        } else {
            events = Collections.emptyList();
        }

        List<Map<String, Object>> result = events.stream().map(e -> {
            com.manacommunity.api.sports.model.SportsAuctionConfig ac = auctionConfigRepo.findByEventId(e.getId()).orElse(null);
            Integer totalTeams = ac != null && ac.getTotalTeams() != null ? ac.getTotalTeams() : 0;
            String venueStr = "";
            if (e.getVenue() != null) {
                venueStr = e.getVenue().getName();
                if (e.getVenue().getCity() != null) venueStr += ", " + e.getVenue().getCity();
                if (e.getVenue().getArea() != null) venueStr += ", " + e.getVenue().getArea();
                if (e.getVenue().getAddress() != null) venueStr += " - " + e.getVenue().getAddress();
                if (e.getVenue().getMapLink() != null) venueStr += " (" + e.getVenue().getMapLink() + ")";
            }

            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", e.getId());
            map.put("name", e.getName() != null ? e.getName() : "");
            map.put("tournamentId", e.getTournament() != null ? e.getTournament().getId() : null);
            map.put("tournamentName", e.getTournament() != null && e.getTournament().getName() != null ? e.getTournament().getName() : "");
            map.put("sportId", e.getSport() != null ? e.getSport().getId() : 0L);
            map.put("sportName", e.getSport() != null ? e.getSport().getName() : "");
            map.put("communityId", e.getCommunity() != null ? e.getCommunity().getId() : 0L);
            map.put("communityName", e.getCommunity() != null ? e.getCommunity().getName() : "");
            map.put("eventDateStart", e.getEventDateStart() != null ? e.getEventDateStart().toString() : "");
            map.put("eventDateEnd", e.getEventDateEnd() != null ? e.getEventDateEnd().toString() : "");
            map.put("status", e.getTournament() != null && e.getTournament().getRegistrationStatus() != null ? e.getTournament().getRegistrationStatus().name() : "");
            map.put("totalTeams", totalTeams);
            map.put("venueId", e.getVenue() != null ? e.getVenue().getId() : 0L);
            map.put("venueName", venueStr);
            map.put("gender", e.getGender() != null ? e.getGender() : "ALL");
            map.put("minAge", e.getMinAge() != null ? e.getMinAge() : 0);
            map.put("maxAge", e.getMaxAge() != null ? e.getMaxAge() : 100);
            String catName = "";
            if (e.getCategories() != null && !e.getCategories().isEmpty()) {
                catName = e.getCategories().iterator().next().getName();
            }
            map.put("categoryName", catName);
            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/tournament/tournaments
     * Returns all tournaments for the current community for dropdown selection.
     */
    @GetMapping("/tournaments")
    public ResponseEntity<List<Map<String, Object>>> getTournamentsForDropdown(
            @RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (communityId == null && principal != null) {
            AppUser currentUser = loggedInUserService.resolve(principal);
            if (currentUser != null && currentUser.getCommunity() != null) {
                communityId = currentUser.getCommunity().getId();
            }
        }

        List<SportsTournament> tournaments;
        if (communityId != null) {
            tournaments = tournamentRepo.findByEventCommunityIdOrderByCreatedAtDesc(communityId);
        } else {
            tournaments = Collections.emptyList();
        }

        List<Map<String, Object>> result = tournaments.stream().map(t -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", t.getId());
            map.put("name", t.getName() != null ? t.getName() : "");
            map.put("eventDateStart", t.getEventDateStart() != null ? t.getEventDateStart().toString() : "");
            map.put("eventDateEnd", t.getEventDateEnd() != null ? t.getEventDateEnd().toString() : "");
            map.put("maxParticipants", t.getMaxParticipants() != null ? t.getMaxParticipants() : 0);
            map.put("status", t.getRegistrationStatus() != null ? t.getRegistrationStatus().name() : "");
            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/tournament/types
     * Returns all supported tournament types with descriptions.
     */
    @GetMapping("/types")
    public ResponseEntity<List<TournamentTypeInfo>> getTournamentTypes() {
        return ResponseEntity.ok(List.of(
            new TournamentTypeInfo("KNOCKOUT",          "Knockout",
                "Single elimination — lose once and you're out. Fast and decisive.",
                "2-64", "ceil(log2 N) rounds"),
                new TournamentTypeInfo("GROUP_KNOCKOUT",    "Group Stage + Knockout",
                "Teams split into groups (A, B …). Top N advance to knockout bracket.",
                "4-32", "Group rounds + Knockout"),
            new TournamentTypeInfo("ROUND_ROBIN",       "Round Robin (League)",
                "Everyone plays everyone once. Winner by most points (NRR tiebreak).",
                "3-20", "N*(N-1)/2 matches"),
            new TournamentTypeInfo("DOUBLE_ELIMINATION","Double Elimination",
                "Winners bracket + Losers bracket. Need two losses to be eliminated.",
                "4-32", "~2x Knockout rounds"),
            new TournamentTypeInfo("SWISS",             "Swiss System",
                "Players paired by score each round. No rematches. Used in Chess.",
                "4-128", "ceil(log2 N) rounds"),
            new TournamentTypeInfo("SUPER_LEAGUE",      "Super League (IPL-style)",
                "Full round-robin league → top 4 in playoffs (Q1/Eliminator/Q2/Final).",
                "6-10", "League + 4 playoff matches")
        ));
    }

    // ═══════════════════════════════════════════════════════════════
    // SCHEDULE GENERATION & BRACKET
    // ═══════════════════════════════════════════════════════════════

    /**
     * POST /api/tournament/schedule
     * Validates config, generates all matches, returns full schedule.
     */
    @PostMapping("/schedule")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<SportsTournamentScheduleResponse> createSchedule(
            @Valid @RequestBody SportsTournamentConfigRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Creating tournament schedule userId={}", principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(schedulerService.createTournamentSchedule(req, principal.getId()));
    }

    /**
     * GET /api/tournament/{configId}/schedule
     * Returns full schedule: groups, standings, all matches by round.
     */
    @GetMapping("/{configId}/schedule")
    public ResponseEntity<SportsTournamentScheduleResponse> getSchedule(@PathVariable Long configId) {
        return ResponseEntity.ok(schedulerService.getSchedule(configId));
    }

    /**
     * GET /api/tournament
     * List all tournaments for a community.
     */
    @GetMapping
    public ResponseEntity<List<SportsTournamentConfig>> list(
            @RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long targetCommunityId = communityId;
        if (!user.hasRole(ROLE_SUPER_ADMIN)) {
            targetCommunityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        }
        if (targetCommunityId == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(configRepo.findByCommunityIdOrderByCreatedAtDesc(targetCommunityId));
    }

    /**
     * POST /api/tournament/match/result
     * Records result, updates standings/NRR, advances bracket.
     */
    @PostMapping("/match/result")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SPORTS_REFEREE','SUPER_ADMIN')")
    public ResponseEntity<SportsTournamentScheduleResponse> recordResult(
            @Valid @RequestBody SportsMatchResultRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Recording match result matchId={}", req.matchId());
        return ResponseEntity.ok(schedulerService.advanceBracket(req));
    }

    /**
     * POST /api/tournament/{configId}/seed-knockout
     * Seeds advancing teams into knockout bracket after group stage.
     */
    @PostMapping("/{configId}/seed-knockout")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> seedKnockout(@PathVariable Long configId) {
        log.info("Seeding knockout from groups configId={}", configId);
        schedulerService.seedKnockoutFromGroups(configId);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/tournament/{configId}/swiss/next-round
     * Generates next Swiss round pairings.
     */
    @PostMapping("/{configId}/swiss/next-round")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<SportsMatchResponse>> nextSwissRound(@PathVariable Long configId) {
        log.info("Generating next Swiss round configId={}", configId);
        return ResponseEntity.ok(
            schedulerService.generateNextSwissRound(configId)
                .stream().map(schedulerService::toMatchResponse).toList());
    }

    /**
     * PUT /api/tournament/match/{matchId}/reschedule
     */
    @PutMapping("/match/{matchId}/reschedule")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<SportsMatchResponse> rescheduleMatch(
            @PathVariable Long matchId,
            @Valid @RequestBody SportsRescheduleRequest req) {
        log.info("Rescheduling match matchId={}", matchId);
        SportsTournamentMatch updated = schedulerService.rescheduleMatch(matchId, req.scheduledAt(), req.venue());
        return ResponseEntity.ok(schedulerService.toMatchResponse(updated));
    }

    // ═══════════════════════════════════════════════════════════════
    // MANUAL SCHEDULING ENDPOINTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * POST /api/tournament/{configId}/manual/groups/assign
     */
    @PostMapping("/{configId}/manual/groups/assign")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<String> assignTeamsToGroups(
            @PathVariable Long configId,
            @Valid @RequestBody List<GroupAssignmentRequest> assignments) {
        log.info("Assigning teams to groups configId={}", configId);
        schedulerService.assignTeamsToGroups(configId, assignments);
        return ResponseEntity.ok("Group assignments saved successfully.");
    }

    /**
     * POST /api/tournament/{configId}/manual/matches
     */
    @PostMapping("/{configId}/manual/matches")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<SportsMatchResponse> scheduleManualMatch(
            @PathVariable Long configId,
            @Valid @RequestBody SportsMatchScheduleRequest req) {
        log.info("Scheduling manual match configId={} stage={}", configId, req.stage());
        SportsTournamentMatch match = schedulerService.scheduleManualMatch(configId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(schedulerService.toMatchResponse(match));
    }

    /**
     * DELETE /api/tournament/match/{matchId}
     * Deletes a single match.
     */
    @DeleteMapping("/match/{matchId}")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> deleteMatch(@PathVariable Long matchId) {
        log.info("Deleting match matchId={}", matchId);
        schedulerService.deleteMatch(matchId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/tournament/{configId}/matches
     * Returns all saved matches for a config.
     */
    @GetMapping("/{configId}/matches")
    public ResponseEntity<List<SportsMatchResponse>> getMatches(@PathVariable Long configId) {
        SportsTournamentConfig config = configRepo.findById(configId)
            .or(() -> configRepo.findByEventId(configId).stream().findFirst())
            .orElse(null);
        Long targetId = config != null ? config.getId() : configId;
        List<SportsMatchResponse> matches = matchRepo.findByConfigIdOrderByScheduledAt(targetId)
            .stream().map(schedulerService::toMatchResponse).toList();
        return ResponseEntity.ok(matches);
    }

    /**
     * POST /api/tournament/{configId}/matches/bulk
     * Replaces all existing matches for the config with the supplied list.
     * Called by the frontend "Save as Draft" and "Save & Publish" buttons.
     */
    @PostMapping("/{configId}/matches/bulk")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> saveMatchesBulk(
            @PathVariable Long configId,
            @Valid @RequestBody SportsBulkMatchSaveRequest request) {
        log.info("Saving matches bulk configId={}", configId);
        int saved = schedulerService.saveMatchesBulk(configId, request.matches());
        return ResponseEntity.ok(Map.of("saved", saved, "configId", configId));
    }

    /**
     * PUT /api/tournament/{configId}/matches/status
     * Updates the status of all matches for a config.
     * Called by "Save as Draft" (DRAFT) and "Save & Publish" (PUBLISHED).
     */
    @PutMapping("/{configId}/matches/status")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateMatchesStatus(
            @PathVariable Long configId,
            @Valid @RequestBody SportsMatchStatusUpdateRequest request) {
        log.info("Updating matches status configId={} status={}", configId, request.status());
        int updated = schedulerService.updateMatchesStatus(configId, request.status());
        return ResponseEntity.ok(Map.of("updated", updated, "configId", configId, "status", request.status()));
    }

    /**
     * DELETE /api/tournament/{configId}/matches
     * Deletes all matches for a config. Called by the "Clear" button.
     */
    @DeleteMapping("/{configId}/matches")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteMatches(@PathVariable Long configId) {
        log.info("Deleting matches configId={}", configId);
        int deleted = schedulerService.deleteMatchesByConfigId(configId);
        return ResponseEntity.ok(Map.of("deleted", deleted, "configId", configId));
    }

    /**
     * POST /api/tournament/schedule/save
     * Unified, deferred save: persists the tournament config and all customized
     * matches together in a single transaction. Replaces the separate
     * createConfig + saveMatchesBulk calls for the scheduler save flow.
     */
    @PostMapping("/schedule/save")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<SportsScheduleSaveResponse> saveSchedule(
            @Valid @RequestBody SportsScheduleSaveRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Saving schedule userId={}", principal.getId());
        return ResponseEntity.ok(schedulerService.saveSchedule(req, principal.getId()));
    }

    /**
     * POST /api/tournament/playoff/generate
     * Stateless: generates the playoff ("rounds to final") bracket from the given
     * inputs and returns the draft. No DB writes — the UI persists via matches/bulk.
     */
    @PostMapping("/playoff/generate")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<SportsPlayoffMatchDraftResponse>> generatePlayoff(
            @Valid @RequestBody SportsPlayoffGenerateRequest request) {
        log.info("Generating playoff bracket");
        return ResponseEntity.ok(playoffGenerator.buildPlayoffBracket(request));
    }

    // ═══════════════════════════════════════════════════════════════
    // DETAILED RESULTS, SCORECARDS & LEADERBOARDS
    // ═══════════════════════════════════════════════════════════════

    /**
     * POST /api/tournament/match/result/detail
     * Records detailed result with scorecard (innings, batting, bowling).
     */
    @PostMapping("/match/result/detail")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SPORTS_REFEREE','SUPER_ADMIN')")
    public ResponseEntity<SportsMatchDetailResponse> recordDetailedResult(
            @Valid @RequestBody SportsMatchResultDetailRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Recording detailed match result");
        return ResponseEntity.ok(matchResultService.recordDetailedResult(req));
    }

    /**
     * GET /api/tournament/match/{matchId}/detail
     * Returns full match detail including scorecard and leaderboard snippet.
     */
    @GetMapping("/match/{matchId}/detail")
    public ResponseEntity<SportsMatchDetailResponse> getMatchDetail(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchResultService.getMatchDetail(matchId));
    }

    /**
     * GET /api/tournament/{configId}/leaderboard?type=runs|wickets|mvp
     * Returns tournament leaderboard.
     */
    @GetMapping("/{configId}/leaderboard")
    public ResponseEntity<List<SportsMatchDetailResponse.PlayerStatsRow>> getLeaderboard(
            @PathVariable Long configId,
            @RequestParam(defaultValue = "runs") String type) {
        return ResponseEntity.ok(matchResultService.getLeaderboard(configId, type));
    }

    /**
     * GET /api/tournament/player/{playerId}/stats
     * Returns player career stats across all tournaments.
     */
    @GetMapping("/player/{playerId}/stats")
    public ResponseEntity<List<SportsMatchDetailResponse.PlayerStatsRow>> getPlayerStats(
            @PathVariable Long playerId) {
        return ResponseEntity.ok(matchResultService.getPlayerCareerStats(playerId));
    }

    record TournamentTypeInfo(
        String id, String name, String description,
        String teamRange, String formatNote) {}
}
