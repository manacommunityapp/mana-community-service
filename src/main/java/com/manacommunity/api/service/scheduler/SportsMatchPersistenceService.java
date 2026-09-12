package com.manacommunity.api.service.scheduler;

import com.manacommunity.api.model.Community;

import com.manacommunity.api.user.repository.AppUserRepository;

import com.manacommunity.api.user.model.AppUser;

import com.manacommunity.api.dto.scheduler.*;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.SportsAuctionTeam;
import com.manacommunity.api.model.scheduler.*;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.repository.scheduler.*;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
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
 * Owns all persistence for the scheduler: tournament-config CRUD, the unified
 * transactional schedule save, bulk match writes, status updates, deletes, and
 * manual (drag-and-drop) scheduling. Building a match entity from a UI payload
 * lives here too.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SportsMatchPersistenceService {

    private final SportsTournamentConfigRepository  configRepo;
    private final SportsTournamentGroupRepository   groupRepo;
    private final SportsTournamentMatchRepository   matchRepo;
    private final SportsGroupTeamStandingRepository standingRepo;
    private final SportsAuctionTeamRepository       teamRepo;
    private final AppUserRepository           userRepo;
    private final SportsMetaRepository         sportMetaRepo;
    private final CommunityRepository         communityRepo;
    private final SportsEventRepository       eventRepo;
    private final VenueRepository             venueRepo;
    private final SportsCourtRepository             courtRepo;
    private final SportsTimeSlotAllocator           timeSlots;
    private final AuditService                      auditService;

    // ═══════════════════════════════════════════════════════════════
    // CONFIG CRUD
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public SportsTournamentConfig saveTournamentConfig(SportsTournamentConfigRequest req, Long adminUserId) {
        SportsTournamentConfig config = buildConfig(req, adminUserId);
        config = configRepo.save(config);
        log.info("SportsTournament config saved (DRAFT): [{}] type={}", config.getTournamentName(), config.getTournamentType());
        return config;
    }

    @Transactional
    public SportsTournamentConfig updateTournamentConfig(Long configId, SportsTournamentConfigRequest req) {
        SportsTournamentConfig config = configRepo.findById(configId).orElseThrow(
            () -> new IllegalArgumentException("SportsTournament config not found: " + configId));

        config.setTournamentName(req.tournamentName());
        config.setSport(sportMetaRepo.findById(req.sportId()).orElseThrow(() -> new ResourceNotFoundException("Sport", req.sportId())));
        config.setCommunity(communityRepo.findById(req.communityId()).orElseThrow(() -> new ResourceNotFoundException("Community", req.communityId())));
        if (req.eventId() != null) config.setEvent(eventRepo.findById(req.eventId()).orElseThrow(() -> new ResourceNotFoundException("SportsEvent", req.eventId())));
        config.setTournamentType(TournamentType.valueOf(req.tournamentType()));
        config.setTotalTeams(req.totalTeams());
        config.setNumberOfGroups(req.numberOfGroups());
        config.setTeamsAdvancingPerGroup(req.teamsAdvancingPerGroup());
        config.setSwissRounds(req.swissRounds());
        config.setThirdPlaceMatch(Objects.requireNonNullElse(req.thirdPlaceMatch(), config.getThirdPlaceMatch()));
        config.setHasSeeding(Objects.requireNonNullElse(req.hasSeeding(), config.getHasSeeding()));
        config.setStartDate(req.startDate());
        config.setEndDate(req.endDate());
        config.setMatchDurationMinutes(Objects.requireNonNullElse(req.matchDurationMinutes(), 90));
        config.setBreakBetweenMatchesMinutes(Objects.requireNonNullElse(req.breakBetweenMatchesMinutes(), 30));
        if (req.venueId() != null) config.setVenue(venueRepo.findById(req.venueId()).orElseThrow(() -> new ResourceNotFoundException("Venue", req.venueId())));
        else config.setVenue(null);
        config.setPointsForWin(Objects.requireNonNullElse(req.pointsForWin(), 2));
        config.setPointsForDraw(Objects.requireNonNullElse(req.pointsForDraw(), 1));
        config.setPointsForLoss(Objects.requireNonNullElse(req.pointsForLoss(), 0));
        config = configRepo.save(config);
        log.info("SportsTournament config updated: [{}]", config.getTournamentName());
        return config;
    }

    public SportsTournamentConfigResponse toConfigResponse(SportsTournamentConfig c) {
        int matchCount = matchRepo.findByConfigId(c.getId()).size();
        return new SportsTournamentConfigResponse(
            c.getId(), c.getTournamentName(), c.getTournamentType().name(),
            c.getSport() != null ? c.getSport().getId() : null,
            c.getSport() != null ? c.getSport().getName() : null,
            c.getCommunity() != null ? c.getCommunity().getId() : null,
            c.getCommunity() != null ? c.getCommunity().getName() : null,
            c.getEvent() != null ? c.getEvent().getId() : null,
            c.getEvent() != null ? c.getEvent().getName() : null,
            c.getTotalTeams(), c.getNumberOfGroups(), c.getTeamsPerGroup(),
            c.getTeamsAdvancingPerGroup(), c.getThirdPlaceMatch(), c.getHasSeeding(),
            c.getSwissRounds(), c.getStartDate(), c.getEndDate(),
            c.getMatchDurationMinutes(), c.getBreakBetweenMatchesMinutes(),
            c.getVenue() != null ? c.getVenue().getId() : null,
            c.getVenue() != null ? c.getVenue().getName() : null,
            c.getPointsForWin(), c.getPointsForDraw(),
            c.getPointsForLoss(), c.getStatus().name(), matchCount,
            c.getCreatedAt(), c.getUpdatedAt()
        );
    }

    public List<SportsTournamentConfigResponse> getConfigsByCommunity(Long communityId) {
        return configRepo.findByCommunityIdOrderByCreatedAtDesc(communityId)
            .stream().map(this::toConfigResponse).toList();
    }

    private SportsTournamentConfig buildConfig(SportsTournamentConfigRequest req, Long adminUserId) {
        SportsTournamentConfig.SportsTournamentConfigBuilder builder = SportsTournamentConfig.builder()
            .tournamentName(req.tournamentName())
            .sport(sportMetaRepo.findById(req.sportId()).orElseThrow(() -> new ResourceNotFoundException("Sport", req.sportId())))
            .community(communityRepo.findById(req.communityId()).orElseThrow(() -> new ResourceNotFoundException("Community", req.communityId())))
            .tournamentType(TournamentType.valueOf(req.tournamentType()))
            .totalTeams(req.totalTeams())
            .numberOfGroups(req.numberOfGroups())
            .teamsAdvancingPerGroup(req.teamsAdvancingPerGroup())
            .swissRounds(req.swissRounds())
            .thirdPlaceMatch(Objects.requireNonNullElse(req.thirdPlaceMatch(), true))
            .hasSeeding(Objects.requireNonNullElse(req.hasSeeding(), false))
            .startDate(req.startDate())
            .endDate(req.endDate())
            .matchDurationMinutes(Objects.requireNonNullElse(req.matchDurationMinutes(), 90))
            .breakBetweenMatchesMinutes(Objects.requireNonNullElse(req.breakBetweenMatchesMinutes(), 30))
            .venue(req.venueId() != null ? venueRepo.findById(req.venueId()).orElseThrow(() -> new ResourceNotFoundException("Venue", req.venueId())) : null)
            .pointsForWin(Objects.requireNonNullElse(req.pointsForWin(), 2))
            .pointsForDraw(Objects.requireNonNullElse(req.pointsForDraw(), 1))
            .pointsForLoss(Objects.requireNonNullElse(req.pointsForLoss(), 0))
            .status(SportsTournamentConfig.TournamentStatus.DRAFT)
            .createdBy(userRepo.findById(adminUserId).orElseThrow(() -> new ResourceNotFoundException("AppUser", adminUserId)));

        if (req.eventId() != null) {
            builder.event(eventRepo.findById(req.eventId()).orElseThrow(() -> new ResourceNotFoundException("SportsEvent", req.eventId())));
        }
        return builder.build();
    }

    // ═══════════════════════════════════════════════════════════════
    // MANUAL SCHEDULING (Drag & Drop UI Integration)
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public void assignTeamsToGroups(Long configId, List<GroupAssignmentRequest> assignments) {
        SportsTournamentConfig config = configRepo.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsTournamentConfig", configId));

        // Clear existing group standings for manual mode override
        List<SportsTournamentGroup> groups = groupRepo.findByConfigIdOrderByGroupOrder(configId);
        groups.forEach(g -> standingRepo.deleteAll(standingRepo.findByGroupIdOrderByPointsDescNetRunRateDesc(g.getId())));

        // Group the assignments by groupId
        Map<String, List<GroupAssignmentRequest>> byGroup = assignments.stream()
            .collect(Collectors.groupingBy(GroupAssignmentRequest::groupId));

        int groupOrder = 1;
        for (Map.Entry<String, List<GroupAssignmentRequest>> entry : byGroup.entrySet()) {
            String groupName = entry.getKey();
            if ("UNASSIGNED".equals(groupName)) continue; // don't create a group for unassigned

            // Check if group exists, else create
            SportsTournamentGroup grp = groups.stream()
                .filter(g -> g.getGroupName().equals(groupName))
                .findFirst()
                .orElseGet(() -> {
                    SportsTournamentGroup newGrp = SportsTournamentGroup.builder()
                        .config(config)
                        .groupName(groupName)
                        .groupOrder(0) // update later
                        .build();
                    return groupRepo.save(newGrp);
                });
            grp.setGroupOrder(groupOrder++);
            groupRepo.save(grp);

            for (GroupAssignmentRequest req : entry.getValue()) {
                SportsAuctionTeam team = teamRepo.findById(req.teamId())
                        .orElseThrow(() -> new ResourceNotFoundException("Team", req.teamId()));
                standingRepo.save(SportsGroupTeamStanding.builder()
                    .group(grp)
                    .team(team)
                    .seedRank(0)
                    .build());
            }
        }
    }

    @Transactional
    public void scheduleManualMatch(Long configId, SportsMatchScheduleRequest req) {
        SportsTournamentConfig config = configRepo.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsTournamentConfig", configId));

        SportsAuctionTeam home = req.homeTeamId() != null ? teamRepo.findById(req.homeTeamId()).orElse(null) : null;
        SportsAuctionTeam away = req.awayTeamId() != null ? teamRepo.findById(req.awayTeamId()).orElse(null) : null;

        // Find group if it's a group stage match
        SportsTournamentGroup group = null;
        if ("GROUP_STAGE".equals(req.matchType())) {
            group = groupRepo.findByConfigIdOrderByGroupOrder(configId).stream()
                .filter(g -> g.getGroupName().replace(" ", "_").toUpperCase().equals(req.stage().replace(" ", "_").toUpperCase()))
                .findFirst()
                .orElse(null);
        }

        SportsTournamentMatch m = SportsTournamentMatch.builder()
            .config(config)
            .community(config.getCommunity())
            .group(group)
            .round("GROUP_STAGE".equals(req.matchType()) ? MatchRound.GROUP_STAGE : MatchRound.valueOf(req.stage()))
            .teamA(home)
            .teamB(away)
            .scheduledAt(LocalDateTime.parse(req.startTime()))
            .durationMinutes(config.getMatchDurationMinutes() != null ? config.getMatchDurationMinutes() : 90)
            .venue(config.getVenue())
            .status(MatchStatus.SCHEDULED)
            .build();

        SportsTournamentMatch saved = matchRepo.save(m);

        auditService.record(
            AuditAction.MATCH_SCHEDULED,
            AuditModule.TOURNAMENT,
            "SportsTournamentMatch",
            String.valueOf(saved.getId()),
            null,
            "configId=" + configId + ", round=" + saved.getRound() + ", time=" + saved.getScheduledAt()
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // BULK MATCH SAVE  (Save as Draft / Save & Publish from UI)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Unified, deferred save: persists the config and the full set of customized
     * matches together in ONE transaction (create/update config → mirror status →
     * replace matches). Any failure rolls the whole thing back, so there are never
     * orphaned configs or duplicate match rows.
     */
    @Transactional
    public SportsScheduleSaveResponse saveSchedule(SportsScheduleSaveRequest req, Long adminUserId) {
        // 1. Create or update the tournament config (joins this transaction).
        SportsTournamentConfig config = (req.configId() == null)
            ? saveTournamentConfig(req.config(), adminUserId)
            : updateTournamentConfig(req.configId(), req.config());

        // 2. Mirror the save state onto the config (same mapping as updateMatchesStatus).
        MatchStatus matchStatus = parseStatus(req.status(), MatchStatus.SCHEDULED);
        if (matchStatus == MatchStatus.PUBLISHED) {
            config.setStatus(SportsTournamentConfig.TournamentStatus.ACTIVE);
        } else if (matchStatus == MatchStatus.DRAFT) {
            config.setStatus(SportsTournamentConfig.TournamentStatus.DRAFT);
        }
        config = configRepo.save(config);
        Long configId = config.getId();

        // 3. Replace any existing preview/draft matches for this config.
        //    NOTE: deleteByConfigId is @Modifying(clearAutomatically=true), which clears
        //    the persistence context and detaches `config` (and its lazy associations).
        matchRepo.deleteByConfigId(configId);

        // 4. Re-load the config as a managed entity so building matches and the
        //    response can read lazy fields (sport/community/event) within this session.
        SportsTournamentConfig managed = configRepo.findById(configId).orElseThrow(
            () -> new IllegalArgumentException("SportsTournament config not found: " + configId));

        // 5. Build + persist the new matches. The global DRAFT/PUBLISHED lifecycle status is
        //    applied to ordinary matches, but match-type markers (auto-advanced BYEs) are
        //    preserved so they aren't overwritten to DRAFT/PUBLISHED.
        List<SportsBulkMatchSaveRequest.MatchData> matches =
            req.matches() != null ? req.matches() : List.of();
        List<SportsTournamentMatch> entities = matches.stream()
            .map(m -> buildMatchEntity(managed, m))
            .peek(e -> {
                if (e.getStatus() != MatchStatus.AUTO_ADVANCED && e.getStatus() != MatchStatus.BYE) {
                    e.setStatus(matchStatus);
                }
            })
            .toList();
        matchRepo.saveAll(entities);

        log.info("Unified save: config {} ({}) with {} matches", configId,
            managed.getStatus(), entities.size());

        auditService.record(
            AuditAction.TOURNAMENT_SCHEDULE_SAVED,
            AuditModule.TOURNAMENT,
            "SportsTournamentConfig",
            String.valueOf(configId),
            null,
            "name=" + managed.getTournamentName() + ", status=" + managed.getStatus() + ", matches=" + entities.size()
        );

        return new SportsScheduleSaveResponse(toConfigResponse(managed), entities.size());
    }

    @Transactional
    public int saveMatchesBulk(Long configId, List<SportsBulkMatchSaveRequest.MatchData> matches) {
        SportsTournamentConfig config = configRepo.findById(configId)
            .orElseThrow(() -> new IllegalArgumentException("SportsTournament config not found: " + configId));

        // Replace all existing matches for this config
        matchRepo.deleteByConfigId(configId);

        List<SportsTournamentMatch> entities = matches.stream()
            .map(m -> buildMatchEntity(config, m))
            .toList();

        matchRepo.saveAll(entities);
        log.info("Bulk-saved {} matches for config {}", entities.size(), configId);
        return entities.size();
    }

    /**
     * Updates the status of every match for a config.
     * Used by "Save as Draft" (DRAFT) and "Save & Publish" (PUBLISHED).
     */
    @Transactional
    public int updateMatchesStatus(Long configId, String statusStr) {
        configRepo.findById(configId)
            .orElseThrow(() -> new IllegalArgumentException("SportsTournament config not found: " + configId));

        MatchStatus status = parseStatus(statusStr, MatchStatus.SCHEDULED);
        int updated = matchRepo.updateStatusByConfigId(configId, status);

        // Mirror the publish state on the parent config so listings stay in sync
        SportsTournamentConfig config = configRepo.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsTournamentConfig", configId));
        if (status == MatchStatus.PUBLISHED) {
            config.setStatus(SportsTournamentConfig.TournamentStatus.ACTIVE);
        } else if (status == MatchStatus.DRAFT) {
            config.setStatus(SportsTournamentConfig.TournamentStatus.DRAFT);
        }
        configRepo.save(config);

        log.info("Updated {} matches to status {} for config {}", updated, status, configId);
        return updated;
    }

    /**
     * Deletes all matches for a config — used by the "Clear" button.
     */
    @Transactional
    public int deleteMatchesByConfigId(Long configId) {
        configRepo.findById(configId)
            .orElseThrow(() -> new IllegalArgumentException("SportsTournament config not found: " + configId));

        long count = matchRepo.countByConfigId(configId);
        matchRepo.deleteByConfigId(configId);
        log.info("Deleted {} matches for config {}", count, configId);
        return (int) count;
    }

    private SportsTournamentMatch buildMatchEntity(SportsTournamentConfig config, SportsBulkMatchSaveRequest.MatchData m) {
        MatchRound round = stageToRound(m.stage());

        SportsAuctionTeam teamA = tryResolveTeam(m.homeId());
        SportsAuctionTeam teamB = tryResolveTeam(m.awayId());

        LocalDateTime scheduledAt = timeSlots.parseScheduledAt(m.matchDate(), m.matchTime());

        // Store names + group context in matchNotes so they survive even without linked team entities
        String notes = String.format("{\"homeName\":\"%s\",\"awayName\":\"%s\",\"groupName\":\"%s\"}",
            safe(m.homeName()), safe(m.awayName()), safe(m.groupName()));

        return SportsTournamentMatch.builder()
            .config(config)
            .community(config.getCommunity())
            .round(round)
            .matchNumber(m.matchNumber() != null ? m.matchNumber() : 0)
            .teamA(teamA)
            .teamB(teamB)
            .scheduledAt(scheduledAt)
            .durationMinutes(m.duration() != null ? m.duration() : 60)
            .venue(m.venueId() != null ? venueRepo.findById(m.venueId()).orElseThrow(() -> new ResourceNotFoundException("Venue", m.venueId())) : config.getVenue())
            .court(m.courtId() != null ? courtRepo.findById(m.courtId()).orElseThrow(() -> new ResourceNotFoundException("SportsCourt", m.courtId())) : null)
            .status(parseStatus(m.status(), MatchStatus.SCHEDULED))
            .matchNotes(notes)
            .build();
    }

    /** Safely parse an incoming status string into a MatchStatus, falling back to a default. */
    private MatchStatus parseStatus(String status, MatchStatus fallback) {
        if (status == null || status.isBlank()) return fallback;
        try {
            return MatchStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown match status '{}', defaulting to {}", status, fallback);
            return fallback;
        }
    }

    private MatchRound stageToRound(String stage) {
        if (stage == null) return MatchRound.LEAGUE_MATCH;
        return switch (stage.toUpperCase()) {
            case "GROUP"   -> MatchRound.GROUP_STAGE;
            case "PLAYOFF" -> MatchRound.SEMI_FINAL;
            default        -> MatchRound.FINAL;  // KNOCKOUT
        };
    }

    private SportsAuctionTeam tryResolveTeam(String id) {
        if (id == null || id.isBlank() || "TBD".equalsIgnoreCase(id) || "dummy".equalsIgnoreCase(id)) return null;
        try {
            long teamId = Long.parseLong(id);
            return teamRepo.findById(teamId).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String safe(String s) {
        return s == null ? "" : s.replace("\"", "'");
    }
}
