package com.manacommunity.api.sports.scheduler;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.sports.dto.GroupAssignmentRequest;
import com.manacommunity.api.sports.model.MatchRound;
import com.manacommunity.api.sports.model.MatchStatus;
import com.manacommunity.api.sports.model.SportsCourt;
import com.manacommunity.api.sports.model.SportsEvent;
import com.manacommunity.api.sports.model.SportsTournament;
import com.manacommunity.api.sports.model.TournamentType;
import com.manacommunity.api.sports.model.SportsVenue;
import com.manacommunity.api.sports.repository.SportsAuctionTeamRepository;
import com.manacommunity.api.sports.repository.SportsCourtRepository;
import com.manacommunity.api.sports.repository.SportsEventRepository;
import com.manacommunity.api.sports.repository.SportsMetaRepository;
import com.manacommunity.api.sports.repository.SportsVenueRepository;


import com.manacommunity.api.model.Community;

import com.manacommunity.api.user.repository.AppUserRepository;

import com.manacommunity.api.user.model.AppUser;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.sports.model.SportsAuctionTeam;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
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
    private final SportsVenueRepository             venueRepo;
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

        config.setTournamentName(HtmlSanitizer.sanitizePlainText(req.tournamentName()));
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
            .tournamentName(HtmlSanitizer.sanitizePlainText(req.tournamentName()))
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
    public SportsTournamentConfig resolveConfig(Long configId) {
        if (configId == null) {
            throw new ResourceNotFoundException("SportsTournamentConfig", "id is null");
        }
        SportsTournamentConfig config = configRepo.findById(configId)
                .or(() -> configRepo.findByEventId(configId).stream().findFirst())
                .orElse(null);

        if (config == null) {
            // Check if configId is an Event ID
            SportsEvent event = eventRepo.findById(configId).orElse(null);
            if (event != null) {
                // Auto-provision a default tournament config for this event so operations can proceed seamlessly
                SportsTournamentConfig newConfig = SportsTournamentConfig.builder()
                    .tournamentName(event.getName() != null ? event.getName() : "Event #" + configId)
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
                    .createdAt(LocalDateTime.now())
                    .build();
                return configRepo.save(newConfig);
            }
            throw new ResourceNotFoundException("SportsTournamentConfig", configId);
        }
        return config;
    }

    @Transactional
    public void assignTeamsToGroups(Long configId, List<GroupAssignmentRequest> assignments) {
        SportsTournamentConfig config = resolveConfig(configId);
        Long effectiveConfigId = config.getId();

        // Clear existing group standings for manual mode override
        List<SportsTournamentGroup> groups = groupRepo.findByConfigIdOrderByGroupOrder(effectiveConfigId);
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
    public SportsTournamentMatch scheduleManualMatch(Long configId, SportsMatchScheduleRequest req) {
        SportsTournamentConfig config = resolveConfig(configId);
        Long effectiveConfigId = config.getId();

        SportsAuctionTeam home = req.homeTeamId() != null ? teamRepo.findById(req.homeTeamId()).orElse(null) : null;
        SportsAuctionTeam away = req.awayTeamId() != null ? teamRepo.findById(req.awayTeamId()).orElse(null) : null;

        // Find group if it's a group stage match
        SportsTournamentGroup group = null;
        if ("GROUP_STAGE".equalsIgnoreCase(req.matchType()) && req.stage() != null) {
            String targetStage = req.stage().replace(" ", "_").toUpperCase();
            group = groupRepo.findByConfigIdOrderByGroupOrder(effectiveConfigId).stream()
                .filter(g -> g.getGroupName().replace(" ", "_").toUpperCase().equals(targetStage))
                .findFirst()
                .orElse(null);
        }

        MatchRound round;
        if ("GROUP_STAGE".equalsIgnoreCase(req.matchType())) {
            round = MatchRound.GROUP_STAGE;
        } else if (req.stage() != null) {
            try {
                round = MatchRound.valueOf(req.stage().trim().toUpperCase());
            } catch (Exception e) {
                log.warn("Could not parse MatchRound for stage '{}', defaulting to FINAL", req.stage());
                round = MatchRound.FINAL;
            }
        } else {
            round = MatchRound.FINAL;
        }

        LocalDateTime scheduledAt = parseDateTimeSafe(req.startTime());
        SportsVenue venue = req.venueId() != null
                ? venueRepo.findById(req.venueId()).orElse(config.getVenue())
                : config.getVenue();
        SportsCourt court = req.courtId() != null
                ? courtRepo.findById(req.courtId()).orElse(null)
                : null;
        int duration = req.durationMinutes() != null && req.durationMinutes() > 0
                ? req.durationMinutes()
                : (config.getMatchDurationMinutes() != null ? config.getMatchDurationMinutes() : 60);

        SportsTournamentMatch m = SportsTournamentMatch.builder()
            .config(config)
            .community(config.getCommunity())
            .group(group)
            .round(round)
            .teamA(home)
            .teamB(away)
            .scheduledAt(scheduledAt)
            .durationMinutes(duration)
            .venue(venue)
            .court(court)
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

        return saved;
    }

    @Transactional
    public void deleteMatch(Long matchId) {
        if (matchId == null) return;
        matchRepo.deleteById(matchId);
        log.info("Deleted tournament match id={}", matchId);
    }

    private LocalDateTime parseDateTimeSafe(String raw) {
        if (raw == null || raw.isBlank()) return LocalDateTime.now();
        String cleaned = raw.trim();
        if (cleaned.endsWith("Z")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        if (cleaned.contains(".")) {
            int dotIdx = cleaned.indexOf(".");
            cleaned = cleaned.substring(0, dotIdx);
        }
        try {
            return LocalDateTime.parse(cleaned);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(cleaned.replace(" ", "T"));
            } catch (Exception ex) {
                log.warn("Could not parse datetime '{}', falling back to now: {}", raw, ex.getMessage());
                return LocalDateTime.now();
            }
        }
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

        // 5. Sync groups and build + persist the new matches. The global DRAFT/PUBLISHED lifecycle status is
        //    applied to ordinary matches, but match-type markers (auto-advanced BYEs) are
        //    preserved so they aren't overwritten to DRAFT/PUBLISHED.
        List<SportsBulkMatchSaveRequest.MatchData> matches =
            req.matches() != null ? req.matches() : List.of();
        Map<String, SportsTournamentGroup> groupMap = syncGroupsForConfig(managed, matches);
        List<SportsTournamentMatch> entities = matches.stream()
            .map(m -> buildMatchEntity(managed, m, groupMap))
            .peek(e -> {
                if (e.getStatus() != MatchStatus.AUTO_ADVANCED && e.getStatus() != MatchStatus.BYE) {
                    e.setStatus(matchStatus);
                }
            })
            .toList();
        List<SportsTournamentMatch> savedEntities = matchRepo.saveAll(entities);
        if (savedEntities == null || savedEntities.isEmpty()) {
            savedEntities = entities;
        }
        wireAndSaveKnockoutLinks(savedEntities);

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
        SportsTournamentConfig config = resolveConfig(configId);
        Long effectiveConfigId = config.getId();

        // Replace all existing matches for this config
        matchRepo.deleteByConfigId(effectiveConfigId);

        Map<String, SportsTournamentGroup> groupMap = syncGroupsForConfig(config, matches);
        List<SportsTournamentMatch> entities = matches.stream()
            .map(m -> buildMatchEntity(config, m, groupMap))
            .toList();

        List<SportsTournamentMatch> savedEntities = matchRepo.saveAll(entities);
        if (savedEntities == null || savedEntities.isEmpty()) {
            savedEntities = entities;
        }
        wireAndSaveKnockoutLinks(savedEntities);
        log.info("Bulk-saved {} matches for config {}", entities.size(), effectiveConfigId);
        return entities.size();
    }


    /**
     * Updates the status of every match for a config.
     * Used by "Save as Draft" (DRAFT) and "Save & Publish" (PUBLISHED).
     */
    @Transactional
    public int updateMatchesStatus(Long configId, String statusStr) {
        SportsTournamentConfig config = resolveConfig(configId);
        Long effectiveConfigId = config.getId();

        MatchStatus status = parseStatus(statusStr, MatchStatus.SCHEDULED);
        int updated = matchRepo.updateStatusByConfigId(effectiveConfigId, status);

        // Mirror the publish state on the parent config so listings stay in sync
        if (status == MatchStatus.PUBLISHED) {
            config.setStatus(SportsTournamentConfig.TournamentStatus.ACTIVE);
        } else if (status == MatchStatus.DRAFT) {
            config.setStatus(SportsTournamentConfig.TournamentStatus.DRAFT);
        }
        configRepo.save(config);

        log.info("Updated {} matches to status {} for config {}", updated, status, effectiveConfigId);
        return updated;
    }

    /**
     * Deletes all matches for a config — used by the "Clear" button.
     */
    @Transactional
    public int deleteMatchesByConfigId(Long configId) {
        SportsTournamentConfig config = resolveConfig(configId);
        Long effectiveConfigId = config.getId();

        long count = matchRepo.countByConfigId(effectiveConfigId);
        matchRepo.deleteByConfigId(effectiveConfigId);
        log.info("Deleted {} matches for config {}", count, effectiveConfigId);
        return (int) count;
    }

    public void wireAndSaveKnockoutLinks(List<SportsTournamentMatch> savedEntities) {
        if (savedEntities == null || savedEntities.size() < 2) return;

        // Exclude group stages and league matches from bracket advancement wiring
        List<SportsTournamentMatch> koMatches = savedEntities.stream()
            .filter(m -> m.getRound() != MatchRound.GROUP_STAGE && m.getRound() != MatchRound.LEAGUE_MATCH && m.getRound() != MatchRound.THIRD_PLACE)
            .toList();
        if (koMatches.size() < 2) return;

        Map<Integer, List<SportsTournamentMatch>> byRound = koMatches.stream()
            .sorted(Comparator.comparingInt(m -> Objects.requireNonNullElse(m.getBracketSlot(), 0)))
            .collect(Collectors.groupingBy(m -> Objects.requireNonNullElse(m.getRoundNumber(), 1), LinkedHashMap::new, Collectors.toList()));

        List<Integer> roundNums = new ArrayList<>(byRound.keySet());
        Collections.sort(roundNums);

        boolean updated = false;
        for (int rIdx = 0; rIdx < roundNums.size() - 1; rIdx++) {
            int currentRound = roundNums.get(rIdx);
            int nextRound = roundNums.get(rIdx + 1);

            List<SportsTournamentMatch> currentMatches = byRound.get(currentRound);
            List<SportsTournamentMatch> nextMatches = byRound.get(nextRound);

            if (currentMatches == null || nextMatches == null || nextMatches.isEmpty()) continue;

            for (int i = 0; i < currentMatches.size(); i++) {
                SportsTournamentMatch cur = currentMatches.get(i);
                int targetIdx = Math.min(i / 2, nextMatches.size() - 1);
                SportsTournamentMatch nxt = nextMatches.get(targetIdx);

                if (nxt != null && nxt.getId() != null) {
                    cur.setWinnerAdvancesToMatchId(nxt.getId());
                    if (i % 2 == 0) {
                        nxt.setWinnerFeedFromMatchA(cur.getId());
                    } else {
                        nxt.setWinnerFeedFromMatchB(cur.getId());
                    }
                    updated = true;
                }
            }
        }

        if (updated) {
            matchRepo.saveAll(koMatches);
            log.info("Wired winnerAdvancesToMatchId links for {} knockout matches", koMatches.size());
        }
    }

    private Map<String, SportsTournamentGroup> syncGroupsForConfig(SportsTournamentConfig config, List<SportsBulkMatchSaveRequest.MatchData> matches) {
        Map<String, SportsTournamentGroup> groupMap = new HashMap<>();
        List<SportsTournamentGroup> existing = groupRepo.findByConfigIdOrderByGroupOrder(config.getId());
        for (SportsTournamentGroup g : existing) {
            groupMap.put(g.getGroupName(), g);
        }
        int nextOrder = existing.size() + 1;
        for (SportsBulkMatchSaveRequest.MatchData m : matches) {
            String gName = m.groupName();
            if (gName != null && !gName.isBlank() &&
                ("GROUP".equalsIgnoreCase(m.stage()) || "ROUND_ROBIN".equalsIgnoreCase(m.stage()) || gName.startsWith("Group") || gName.startsWith("Pool"))) {
                if (!groupMap.containsKey(gName)) {
                    SportsTournamentGroup grp = SportsTournamentGroup.builder()
                        .config(config)
                        .groupName(gName)
                        .groupOrder(nextOrder++)
                        .build();
                    grp = groupRepo.save(grp);
                    groupMap.put(gName, grp);
                }
            }
        }

        // Pre-seed standings rows for teams in each group to prevent NPE on first match completion
        for (SportsBulkMatchSaveRequest.MatchData m : matches) {
            String gName = m.groupName();
            if (gName != null && groupMap.containsKey(gName)) {
                SportsTournamentGroup grp = groupMap.get(gName);
                SportsAuctionTeam teamA = tryResolveTeam(m.homeId());
                SportsAuctionTeam teamB = tryResolveTeam(m.awayId());
                if (teamA != null && standingRepo.findByGroupIdAndTeamId(grp.getId(), teamA.getId()) == null) {
                    standingRepo.save(SportsGroupTeamStanding.builder()
                        .group(grp).team(teamA).seedRank(0)
                        .played(0).won(0).lost(0).drawn(0).points(0).netRunRate(0.0)
                        .build());
                }
                if (teamB != null && standingRepo.findByGroupIdAndTeamId(grp.getId(), teamB.getId()) == null) {
                    standingRepo.save(SportsGroupTeamStanding.builder()
                        .group(grp).team(teamB).seedRank(0)
                        .played(0).won(0).lost(0).drawn(0).points(0).netRunRate(0.0)
                        .build());
                }
            }
        }

        return groupMap;
    }

    private SportsTournamentMatch buildMatchEntity(SportsTournamentConfig config, SportsBulkMatchSaveRequest.MatchData m) {
        return buildMatchEntity(config, m, null);
    }

    private SportsTournamentMatch buildMatchEntity(
            SportsTournamentConfig config,
            SportsBulkMatchSaveRequest.MatchData m,
            Map<String, SportsTournamentGroup> groupMap) {

        MatchRound round = resolveMatchRound(m);
        int roundNumber = resolveRoundNumber(m, round);

        SportsAuctionTeam teamA = tryResolveTeam(m.homeId());
        SportsAuctionTeam teamB = tryResolveTeam(m.awayId());

        LocalDateTime scheduledAt = timeSlots.parseScheduledAt(m.matchDate(), m.matchTime());

        // Store names + flats + group context in matchNotes so they survive even without linked team entities
        String notes = String.format("{\"homeName\":\"%s\",\"homeFlat\":\"%s\",\"awayName\":\"%s\",\"awayFlat\":\"%s\",\"groupName\":\"%s\"}",
            safe(m.homeName()), safe(m.homeFlat()), safe(m.awayName()), safe(m.awayFlat()), safe(m.groupName()));

        SportsVenue venue = config.getVenue();
        if (m.venueId() != null) {
            venue = venueRepo.findById(m.venueId()).orElse(config.getVenue());
        }

        SportsCourt court = null;
        if (m.courtId() != null) {
            court = courtRepo.findById(m.courtId()).orElse(null);
        }

        SportsTournamentGroup group = null;
        if (groupMap != null && m.groupName() != null) {
            group = groupMap.get(m.groupName());
        }

        return SportsTournamentMatch.builder()
            .config(config)
            .community(config.getCommunity())
            .group(group)
            .round(round)
            .roundNumber(roundNumber)
            .bracketSlot(m.bracketSlot() != null ? m.bracketSlot() : 0)
            .matchNumber(m.matchNumber() != null ? m.matchNumber() : 0)
            .teamA(teamA)
            .teamB(teamB)
            .scheduledAt(scheduledAt)
            .durationMinutes(m.duration() != null ? m.duration() : 60)
            .venue(venue)
            .court(court)
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

    private MatchRound resolveMatchRound(SportsBulkMatchSaveRequest.MatchData m) {

        if (m.round() != null && !m.round().isBlank()) {
            try {
                return MatchRound.valueOf(m.round().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        String name = (m.groupName() != null ? m.groupName() : "").toUpperCase();
        if (name.contains("THIRD_PLACE") || name.contains("3RD PLACE") || name.contains("THIRD PLACE")) {
            return MatchRound.THIRD_PLACE;
        }
        if (name.contains("FINAL") && !name.contains("SEMI") && !name.contains("QUARTER")) {
            return MatchRound.FINAL;
        }
        if (name.contains("SEMI")) {
            return MatchRound.SEMI_FINAL;
        }
        if (name.contains("QUARTER")) {
            return MatchRound.QUARTER_FINAL;
        }
        if (name.contains("ROUND OF 16") || name.contains("ROUND_OF_16") || name.contains("R16")) {
            return MatchRound.ROUND_OF_16;
        }
        if (name.contains("ROUND OF 32") || name.contains("ROUND_OF_32") || name.contains("R32")) {
            return MatchRound.ROUND_OF_32;
        }
        if (name.contains("ROUND OF 64") || name.contains("ROUND_OF_64") || name.contains("R64")) {
            return MatchRound.ROUND_OF_64;
        }

        if (m.stage() == null) return MatchRound.LEAGUE_MATCH;
        return switch (m.stage().toUpperCase()) {
            case "GROUP"       -> MatchRound.GROUP_STAGE;
            case "ROUND_ROBIN" -> MatchRound.LEAGUE_MATCH;
            case "PLAYOFF"     -> MatchRound.SEMI_FINAL;
            default            -> MatchRound.FINAL;
        };
    }

    private int resolveRoundNumber(SportsBulkMatchSaveRequest.MatchData m, MatchRound round) {
        if (m.roundNumber() != null && m.roundNumber() > 0) {
            return m.roundNumber();
        }
        return switch (round) {
            case ROUND_OF_64        -> 1;
            case ROUND_OF_32        -> 2;
            case ROUND_OF_16        -> 3;
            case QUARTER_FINAL      -> 4;
            case SEMI_FINAL         -> 5;
            case FINAL, THIRD_PLACE -> 6;
            case GROUP_STAGE, LEAGUE_MATCH -> 1;
            default                 -> 1;
        };
    }


    private SportsAuctionTeam tryResolveTeam(String id) {
        if (id == null || id.isBlank() || "TBD".equalsIgnoreCase(id) || "dummy".equalsIgnoreCase(id)) return null;
        try {
            long teamId = Long.parseLong(id);
            return teamRepo.findById(teamId).orElse(null);
        } catch (NumberFormatException e) {
            log.debug("Non-numeric team id '{}', skipping team resolution", id);
            return null;
        }
    }

    private String safe(String s) {
        return s == null ? "" : s.replace("\"", "'");
    }
}
