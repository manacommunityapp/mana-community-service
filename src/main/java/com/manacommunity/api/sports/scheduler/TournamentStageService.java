package com.manacommunity.api.sports.scheduler;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.sports.model.MatchRound;
import com.manacommunity.api.sports.model.MatchStatus;
import com.manacommunity.api.sports.model.SportsAuctionTeam;
import com.manacommunity.api.sports.model.TournamentType;
import com.manacommunity.api.sports.repository.*;
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
public class TournamentStageService {

    private final SportsTournamentStageRepository stageRepo;
    private final SportsScheduleVersionRepository versionRepo;
    private final SportsMatchPostponementRepository postponementRepo;
    private final SportsTournamentReserveDayRepository reserveDayRepo;
    private final SportsTournamentConfigRepository configRepo;
    private final SportsTournamentMatchRepository matchRepo;
    private final SportsBracketGenerator bracketGenerator;
    private final SportsAuctionTeamRepository teamRepo;
    private final SportsAuctionConfigRepository auctionConfigRepo;
    private final SportsMatchPersistenceService matchPersistence;

    public SportsTournamentConfig resolveConfig(Long configId) {
        if (configId == null) return null;
        return configRepo.findById(configId)
                .or(() -> configRepo.findByEventId(configId).stream().findFirst())
                .orElse(null);
    }

    // ─── Stage CRUD ──────────────────────────────────────────────────────────

    @Transactional
    public List<StageResponse> getStages(Long configId) {
        SportsTournamentConfig config = resolveConfig(configId);
        if (config == null) {
            return Collections.emptyList();
        }
        Long effectiveConfigId = config.getId();
        List<SportsTournamentStage> stages = stageRepo.findByConfigIdOrderByStageOrder(effectiveConfigId);
        if (stages.isEmpty()) {
            stages = autoDetectOrCreateStages(config);
        }
        return stages.stream().map(s -> toStageResponse(s, effectiveConfigId)).collect(Collectors.toList());
    }

    @Transactional
    public StageResponse createStage(Long configId, StageCreateRequest req) {
        SportsTournamentConfig config = resolveConfig(configId);
        if (config == null) {
            throw new ResourceNotFoundException("Tournament config not found: " + configId);
        }

        SportsTournamentStage stage = SportsTournamentStage.builder()
                .config(config)
                .stageName(req.stageName())
                .stageType(req.stageType())
                .stageOrder(req.stageOrder())
                .schedulingMode(req.schedulingMode() != null ? req.schedulingMode() : SportsTournamentStage.SchedulingMode.HYBRID)
                .startDate(req.startDate())
                .endDate(req.endDate())
                .build();

        SportsTournamentStage saved = stageRepo.save(stage);
        log.info("Created stage id={} name={} for config={}", saved.getId(), saved.getStageName(), config.getId());
        return toStageResponse(saved, config.getId());
    }

    @Transactional
    public StageResponse updateStage(Long stageId, StageUpdateRequest req) {
        SportsTournamentStage stage = findStageOrThrow(stageId);
        if (req.stageName() != null) stage.setStageName(req.stageName());
        if (req.stageType() != null) stage.setStageType(req.stageType());
        if (req.stageOrder() != null) stage.setStageOrder(req.stageOrder());
        if (req.schedulingMode() != null) stage.setSchedulingMode(req.schedulingMode());
        if (req.startDate() != null) stage.setStartDate(req.startDate());
        if (req.endDate() != null) stage.setEndDate(req.endDate());

        SportsTournamentStage saved = stageRepo.save(stage);
        return toStageResponse(saved, stage.getConfig().getId());
    }

    @Transactional
    public void deleteStage(Long stageId) {
        SportsTournamentStage stage = findStageOrThrow(stageId);
        if (stage.getStageStatus() != SportsTournamentStage.StageStatus.NOT_GENERATED
                && stage.getStageStatus() != SportsTournamentStage.StageStatus.DRAFT) {
            throw new IllegalStateException("Cannot delete a stage with status: " + stage.getStageStatus());
        }
        stageRepo.delete(stage);
        log.info("Deleted stage id={}", stageId);
    }

    // ─── Schedule Generation ─────────────────────────────────────────────────

    @Transactional
    public GenerateResult generateStageSchedule(Long stageId, GenerateRequest req) {
        SportsTournamentStage stage = findStageOrThrow(stageId);
        if (stage.getStageStatus() != SportsTournamentStage.StageStatus.NOT_GENERATED
                && stage.getStageStatus() != SportsTournamentStage.StageStatus.DRAFT) {
            throw new IllegalStateException("Stage already has generated schedule. Status: " + stage.getStageStatus());
        }

        Long configId = stage.getConfig().getId();

        // Multi-level resolution to find matches belonging to this stage
        List<SportsTournamentMatch> matches = findMatchesForStage(stage);

        int nextVersion = versionRepo.findMaxVersionNumber(configId) + 1;
        SportsScheduleVersion version = SportsScheduleVersion.builder()
                .config(stage.getConfig())
                .stage(stage)
                .versionNumber(nextVersion)
                .changeReason(matches.isEmpty() ? "Stage generation (no matches found)" : "Stage fixture generation")
                .build();
        versionRepo.save(version);

        List<String> conflicts = new ArrayList<>();
        List<Long> linkedMatchIds = new ArrayList<>();

        for (SportsTournamentMatch match : matches) {
            match.setStageId(stage.getId());
            match.setScheduleVersionId(version.getId());
            linkedMatchIds.add(match.getId());
        }
        if (!matches.isEmpty()) {
            matchRepo.saveAll(matches);
        }

        if (linkedMatchIds.isEmpty()) {
            conflicts.add("No matches found for stage '" + stage.getStageName() + "' (Type: " + stage.getStageType()
                    + "). Matches must first be created via Setup Schedule or auto-generated.");
        }

        version.setChangedMatchIdsJson(linkedMatchIds.toString());
        versionRepo.save(version);

        stage.setStageStatus(SportsTournamentStage.StageStatus.DRAFT);
        stage.setPublishedVersionId(version.getId());
        if (req != null && req.schedulingMode() != null) {
            stage.setSchedulingMode(req.schedulingMode());
        }
        stageRepo.save(stage);

        log.info("Generated schedule for stage id={} name='{}' version={} linkedMatches={}", stageId, stage.getStageName(), nextVersion, linkedMatchIds.size());
        return new GenerateResult(linkedMatchIds.size(), conflicts);
    }

    private List<SportsTournamentMatch> findMatchesForStage(SportsTournamentStage stage) {
        Long configId = stage.getConfig().getId();

        // 1. Matches already assigned to this stage
        List<SportsTournamentMatch> alreadyAssigned = matchRepo.findByStageId(stage.getId());
        if (!alreadyAssigned.isEmpty()) {
            return alreadyAssigned;
        }

        // 2. If no matches exist at all for this config, auto-generate them
        if (matchRepo.countByConfigId(configId) == 0) {
            autoGenerateTournamentMatches(stage.getConfig());
        }

        // 3. Match by candidate rounds
        List<MatchRound> candidateRounds = getCandidateRounds(stage.getStageType());
        if (!candidateRounds.isEmpty()) {
            List<SportsTournamentMatch> matches = matchRepo.findUnassignedByConfigIdAndRounds(configId, candidateRounds);
            if (!matches.isEmpty()) {
                return matches;
            }
        }

        // 4. Match by stage name heuristics
        String name = stage.getStageName() != null ? stage.getStageName().toLowerCase() : "";
        List<MatchRound> heuristicRounds = new ArrayList<>();
        if (name.contains("round of 64") || name.contains("round 64") || name.contains("r64")) {
            heuristicRounds.add(MatchRound.ROUND_OF_64);
        } else if (name.contains("round of 32") || name.contains("round 32") || name.contains("r32")) {
            heuristicRounds.add(MatchRound.ROUND_OF_32);
        } else if (name.contains("round of 16") || name.contains("round 16") || name.contains("r16") || name.contains("pre-quarter")) {
            heuristicRounds.add(MatchRound.ROUND_OF_16);
        } else if (name.contains("quarter")) {
            heuristicRounds.add(MatchRound.QUARTER_FINAL);
        } else if (name.contains("semi")) {
            heuristicRounds.add(MatchRound.SEMI_FINAL);
        } else if (name.contains("third") || name.contains("3rd")) {
            heuristicRounds.add(MatchRound.THIRD_PLACE);
        } else if (name.contains("super final")) {
            heuristicRounds.add(MatchRound.SUPER_FINAL);
            heuristicRounds.add(MatchRound.FINAL);
        } else if (name.contains("final")) {
            heuristicRounds.add(MatchRound.FINAL);
        } else if (name.contains("group") || name.contains("league") || name.contains("pool")) {
            heuristicRounds.add(MatchRound.GROUP_STAGE);
            heuristicRounds.add(MatchRound.LEAGUE_MATCH);
        }

        if (!heuristicRounds.isEmpty()) {
            List<SportsTournamentMatch> matches = matchRepo.findUnassignedByConfigIdAndRounds(configId, heuristicRounds);
            if (!matches.isEmpty()) {
                return matches;
            }
        }

        // 5. Match by stageOrder == roundNumber
        List<SportsTournamentMatch> roundMatches = matchRepo.findByConfigIdAndRoundNumberOrderByMatchNumber(configId, stage.getStageOrder());
        List<SportsTournamentMatch> unassignedRoundMatches = roundMatches.stream()
                .filter(m -> m.getStageId() == null)
                .collect(Collectors.toList());
        if (!unassignedRoundMatches.isEmpty()) {
            return unassignedRoundMatches;
        }

        // 6. If stage is GROUP_STAGE, check unassigned matches with group != null
        if (stage.getStageType() == SportsTournamentStage.StageType.GROUP_STAGE) {
            List<SportsTournamentMatch> groupMatches = matchRepo.findUnassignedByConfigId(configId).stream()
                    .filter(m -> m.getGroup() != null)
                    .collect(Collectors.toList());
            if (!groupMatches.isEmpty()) {
                return groupMatches;
            }
        }

        // 7. If single stage configured for this tournament, link all unassigned
        List<SportsTournamentStage> allStages = stageRepo.findByConfigIdOrderByStageOrder(configId);
        if (allStages.size() == 1) {
            return matchRepo.findUnassignedByConfigId(configId);
        }

        // 8. If multiple stages, and this is the only unassigned stage left
        long unassignedStagesCount = allStages.stream()
                .filter(s -> s.getStageStatus() == SportsTournamentStage.StageStatus.NOT_GENERATED)
                .count();
        if (unassignedStagesCount == 1) {
            return matchRepo.findUnassignedByConfigId(configId);
        }

        // 9. Fallback: unassigned matches for config
        return matchRepo.findUnassignedByConfigId(configId);
    }

    public List<MatchRound> getCandidateRounds(SportsTournamentStage.StageType stageType) {
        if (stageType == null) return List.of();
        return switch (stageType) {
            case GROUP_STAGE -> List.of(MatchRound.GROUP_STAGE, MatchRound.LEAGUE_MATCH);
            case ROUND_OF_64 -> List.of(MatchRound.ROUND_OF_64);
            case ROUND_OF_32 -> List.of(MatchRound.ROUND_OF_32);
            case ROUND_OF_16 -> List.of(MatchRound.ROUND_OF_16);
            case QUARTER_FINAL -> List.of(MatchRound.QUARTER_FINAL);
            case SEMI_FINAL -> List.of(MatchRound.SEMI_FINAL);
            case THIRD_PLACE -> List.of(MatchRound.THIRD_PLACE);
            case FINAL -> List.of(MatchRound.FINAL, MatchRound.SUPER_FINAL);
            case LEAGUE_MATCH -> List.of(MatchRound.LEAGUE_MATCH, MatchRound.GROUP_STAGE);
            case SWISS_ROUND -> List.of(MatchRound.SWISS_ROUND);
            case ELIMINATOR -> List.of(MatchRound.ELIMINATOR);
            case QUALIFIER -> List.of(MatchRound.QUALIFIER_1, MatchRound.QUALIFIER_2);
            case SUPER_FINAL -> List.of(MatchRound.SUPER_FINAL, MatchRound.FINAL);
        };
    }

    private MatchRound mapStageTypeToMatchRound(SportsTournamentStage.StageType stageType) {
        if (stageType == null) return null;
        List<MatchRound> candidates = getCandidateRounds(stageType);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    @Transactional
    public void autoGenerateTournamentMatches(SportsTournamentConfig config) {
        List<SportsAuctionTeam> teams = teamRepo.findByConfigIdOrderByTeamName(config.getId());
        if (teams.isEmpty()) {
            Long eventId = (config.getEvent() != null) ? config.getEvent().getId() : null;
            if (eventId != null) {
                teams = teamRepo.findByEventIdOrderByTeamName(eventId);
                if (teams.isEmpty()) {
                    var ac = auctionConfigRepo.findByEventId(eventId).orElse(null);
                    if (ac != null) {
                        teams = teamRepo.findByConfigIdOrderByTeamName(ac.getId());
                    }
                }
            }
        }

        int requiredTeams = config.getTotalTeams() != null && config.getTotalTeams() > 0 ? config.getTotalTeams() : Math.max(teams.size(), 2);
        List<SportsAuctionTeam> effectiveTeams = new ArrayList<>(teams);
        while (effectiveTeams.size() < requiredTeams) {
            SportsAuctionTeam placeholder = SportsAuctionTeam.builder()
                    .teamName("Team " + (effectiveTeams.size() + 1))
                    .community(config.getCommunity())
                    .event(config.getEvent())
                    .build();
            placeholder = teamRepo.save(placeholder);
            effectiveTeams.add(placeholder);
        }

        List<SportsTournamentMatch> matches = bracketGenerator.generate(config, effectiveTeams);
        List<SportsTournamentMatch> saved = matchRepo.saveAll(matches);
        matchPersistence.wireAndSaveKnockoutLinks(saved);
        log.info("Auto-generated {} matches for tournament config id={}", saved.size(), config.getId());
    }

    @Transactional
    public List<SportsTournamentStage> autoDetectOrCreateStages(SportsTournamentConfig config) {
        List<SportsTournamentStage> stages = new ArrayList<>();
        List<SportsTournamentMatch> matches = matchRepo.findByConfigId(config.getId());

        if (!matches.isEmpty()) {
            // Group matches by round and create stages corresponding to distinct rounds
            Map<MatchRound, List<SportsTournamentMatch>> byRound = matches.stream()
                    .filter(m -> m.getRound() != null)
                    .collect(Collectors.groupingBy(SportsTournamentMatch::getRound, LinkedHashMap::new, Collectors.toList()));

            // Sort rounds by earliest roundNumber
            List<Map.Entry<MatchRound, List<SportsTournamentMatch>>> sortedEntries = new ArrayList<>(byRound.entrySet());
            sortedEntries.sort(Comparator.comparingInt(e -> e.getValue().stream().mapToInt(SportsTournamentMatch::getRoundNumber).min().orElse(1)));

            int order = 1;
            for (Map.Entry<MatchRound, List<SportsTournamentMatch>> entry : sortedEntries) {
                MatchRound r = entry.getKey();
                SportsTournamentStage.StageType sType = switch (r) {
                    case GROUP_STAGE -> SportsTournamentStage.StageType.GROUP_STAGE;
                    case ROUND_OF_64 -> SportsTournamentStage.StageType.ROUND_OF_64;
                    case ROUND_OF_32 -> SportsTournamentStage.StageType.ROUND_OF_32;
                    case ROUND_OF_16 -> SportsTournamentStage.StageType.ROUND_OF_16;
                    case QUARTER_FINAL -> SportsTournamentStage.StageType.QUARTER_FINAL;
                    case SEMI_FINAL -> SportsTournamentStage.StageType.SEMI_FINAL;
                    case THIRD_PLACE -> SportsTournamentStage.StageType.THIRD_PLACE;
                    case FINAL, GRAND_FINAL -> SportsTournamentStage.StageType.FINAL;
                    case SUPER_FINAL -> SportsTournamentStage.StageType.SUPER_FINAL;
                    case LEAGUE_MATCH -> SportsTournamentStage.StageType.LEAGUE_MATCH;
                    case SWISS_ROUND -> SportsTournamentStage.StageType.SWISS_ROUND;
                    case ELIMINATOR, LOSERS_BRACKET -> SportsTournamentStage.StageType.ELIMINATOR;
                    case QUALIFIER_1, QUALIFIER_2 -> SportsTournamentStage.StageType.QUALIFIER;
                    case WINNERS_BRACKET -> SportsTournamentStage.StageType.QUARTER_FINAL;
                };

                String sName = switch (r) {
                    case GROUP_STAGE -> "Group Stage";
                    case ROUND_OF_64 -> "Round of 64";
                    case ROUND_OF_32 -> "Round of 32";
                    case ROUND_OF_16 -> "Round of 16";
                    case QUARTER_FINAL -> "Quarter-finals";
                    case SEMI_FINAL -> "Semi-finals";
                    case THIRD_PLACE -> "Third-place Match";
                    case FINAL -> "Final";
                    case GRAND_FINAL -> "Grand Final";
                    case SUPER_FINAL -> "Super Final";
                    case LEAGUE_MATCH -> "League Stage";
                    case SWISS_ROUND -> "Swiss Round";
                    case ELIMINATOR -> "Eliminator";
                    case LOSERS_BRACKET -> "Elimination Bracket";
                    case QUALIFIER_1, QUALIFIER_2 -> "Qualifier";
                    case WINNERS_BRACKET -> "Winners Bracket";
                };

                stages.add(SportsTournamentStage.builder()
                        .config(config)
                        .stageName(sName)
                        .stageType(sType)
                        .stageOrder(order++)
                        .schedulingMode(SportsTournamentStage.SchedulingMode.HYBRID)
                        .stageStatus(SportsTournamentStage.StageStatus.NOT_GENERATED)
                        .publicationStatus(SportsTournamentStage.PublicationStatus.UNPUBLISHED)
                        .build());
            }
        }

        if (stages.isEmpty()) {
            TournamentType tType = config.getTournamentType() != null ? config.getTournamentType() : TournamentType.KNOCKOUT_SINGLE;
            int total = config.getTotalTeams() != null && config.getTotalTeams() > 0 ? config.getTotalTeams() : 8;

            int order = 1;
            switch (tType) {
                case KNOCKOUT_SINGLE, KNOCKOUT, CUSTOM -> {
                    if (total > 32) stages.add(createDefaultStage(config, "Round of 64", SportsTournamentStage.StageType.ROUND_OF_64, order++));
                    if (total > 16) stages.add(createDefaultStage(config, "Round of 32", SportsTournamentStage.StageType.ROUND_OF_32, order++));
                    if (total > 8)  stages.add(createDefaultStage(config, "Round of 16", SportsTournamentStage.StageType.ROUND_OF_16, order++));
                    if (total > 4)  stages.add(createDefaultStage(config, "Quarter-finals", SportsTournamentStage.StageType.QUARTER_FINAL, order++));
                    if (total > 2)  stages.add(createDefaultStage(config, "Semi-finals", SportsTournamentStage.StageType.SEMI_FINAL, order++));
                    if (Boolean.TRUE.equals(config.getThirdPlaceMatch())) {
                        stages.add(createDefaultStage(config, "Third-place Match", SportsTournamentStage.StageType.THIRD_PLACE, order++));
                    }
                    stages.add(createDefaultStage(config, "Final", SportsTournamentStage.StageType.FINAL, order++));
                }
                case GROUP_KNOCKOUT, GROUP_PLAYOFF -> {
                    stages.add(createDefaultStage(config, "Group Stage", SportsTournamentStage.StageType.GROUP_STAGE, order++));
                    if (total > 4) stages.add(createDefaultStage(config, "Quarter-finals", SportsTournamentStage.StageType.QUARTER_FINAL, order++));
                    stages.add(createDefaultStage(config, "Semi-finals", SportsTournamentStage.StageType.SEMI_FINAL, order++));
                    stages.add(createDefaultStage(config, "Final", SportsTournamentStage.StageType.FINAL, order++));
                }
                case ROUND_ROBIN, LEAGUE -> {
                    stages.add(createDefaultStage(config, "League Stage", SportsTournamentStage.StageType.LEAGUE_MATCH, order++));
                }
                case DOUBLE_ELIMINATION, KNOCKOUT_DOUBLE -> {
                    stages.add(createDefaultStage(config, "Winners Bracket", SportsTournamentStage.StageType.QUARTER_FINAL, order++));
                    stages.add(createDefaultStage(config, "Elimination Bracket", SportsTournamentStage.StageType.ELIMINATOR, order++));
                    stages.add(createDefaultStage(config, "Grand Final", SportsTournamentStage.StageType.FINAL, order++));
                }
                case SWISS -> {
                    stages.add(createDefaultStage(config, "Swiss Rounds", SportsTournamentStage.StageType.SWISS_ROUND, order++));
                }
                case SUPER_LEAGUE -> {
                    stages.add(createDefaultStage(config, "Group Stage", SportsTournamentStage.StageType.GROUP_STAGE, order++));
                    stages.add(createDefaultStage(config, "Super League", SportsTournamentStage.StageType.LEAGUE_MATCH, order++));
                    stages.add(createDefaultStage(config, "Final", SportsTournamentStage.StageType.FINAL, order++));
                }
            }
        }

        if (!stages.isEmpty()) {
            stages = stageRepo.saveAll(stages);
            log.info("Auto-created {} stages for tournament config id={}", stages.size(), config.getId());
        }
        return stages;
    }

    private SportsTournamentStage createDefaultStage(SportsTournamentConfig config, String name, SportsTournamentStage.StageType type, int order) {
        return SportsTournamentStage.builder()
                .config(config)
                .stageName(name)
                .stageType(type)
                .stageOrder(order)
                .schedulingMode(SportsTournamentStage.SchedulingMode.HYBRID)
                .stageStatus(SportsTournamentStage.StageStatus.NOT_GENERATED)
                .publicationStatus(SportsTournamentStage.PublicationStatus.UNPUBLISHED)
                .build();
    }

    @Transactional
    public ValidateResult validateStageSchedule(Long stageId) {
        SportsTournamentStage stage = findStageOrThrow(stageId);
        if (stage.getStageStatus() != SportsTournamentStage.StageStatus.DRAFT) {
            throw new IllegalStateException("Can only validate a DRAFT stage");
        }

        stage.setStageStatus(SportsTournamentStage.StageStatus.VALIDATED);
        stageRepo.save(stage);

        log.info("Validated stage id={}", stageId);
        return new ValidateResult(true, List.of(), List.of());
    }

    @Transactional
    public StageResponse approveStageSchedule(Long stageId) {
        SportsTournamentStage stage = findStageOrThrow(stageId);
        if (stage.getStageStatus() != SportsTournamentStage.StageStatus.VALIDATED) {
            throw new IllegalStateException("Can only approve a VALIDATED stage");
        }

        stage.setStageStatus(SportsTournamentStage.StageStatus.APPROVED);
        stageRepo.save(stage);

        log.info("Approved stage id={}", stageId);
        return toStageResponse(stage, stage.getConfig().getId());
    }

    // ─── Publication ─────────────────────────────────────────────────────────

    @Transactional
    public PublicationResult publishStage(Long stageId, String publishedBy) {
        SportsTournamentStage stage = findStageOrThrow(stageId);
        if (stage.getStageStatus() != SportsTournamentStage.StageStatus.APPROVED
                && stage.getStageStatus() != SportsTournamentStage.StageStatus.VALIDATED
                && stage.getStageStatus() != SportsTournamentStage.StageStatus.DRAFT) {
            throw new IllegalStateException("Stage must be DRAFT, VALIDATED or APPROVED to publish");
        }

        // Update all matches linked to this stage to PUBLISHED status
        List<SportsTournamentMatch> stageMatches = matchRepo.findByStageId(stage.getId());
        int publishedCount = 0;
        for (SportsTournamentMatch match : stageMatches) {
            if (match.getStatus() == MatchStatus.SCHEDULED || match.getStatus() == MatchStatus.DRAFT) {
                match.setStatus(MatchStatus.PUBLISHED);
                publishedCount++;
            }
        }
        if (!stageMatches.isEmpty()) {
            matchRepo.saveAll(stageMatches);
        }

        // Update schedule version to PUBLISHED
        if (stage.getPublishedVersionId() != null) {
            versionRepo.findById(stage.getPublishedVersionId()).ifPresent(v -> {
                v.setStatus(SportsScheduleVersion.VersionStatus.PUBLISHED);
                v.setPublishedBy(publishedBy);
                v.setPublishedAt(LocalDateTime.now());
                versionRepo.save(v);
            });
        }

        stage.setStageStatus(SportsTournamentStage.StageStatus.PUBLISHED);
        stage.setPublicationStatus(SportsTournamentStage.PublicationStatus.PUBLISHED);
        stageRepo.save(stage);

        // Promote config from DRAFT to ACTIVE so matches become visible to non-admin users
        SportsTournamentConfig config = stage.getConfig();
        if (config != null && config.getStatus() == SportsTournamentConfig.TournamentStatus.DRAFT) {
            config.setStatus(SportsTournamentConfig.TournamentStatus.ACTIVE);
            configRepo.save(config);
            log.info("Promoted config id={} from DRAFT to ACTIVE on stage publish", config.getId());
        }

        log.info("Published stage id={} by={} matchesPublished={}", stageId, publishedBy, publishedCount);
        return new PublicationResult(stageId, publishedBy, LocalDateTime.now(), "SENT", publishedCount);
    }

    @Transactional
    public StageResponse unpublishStage(Long stageId, String reason) {
        SportsTournamentStage stage = findStageOrThrow(stageId);

        // Revert PUBLISHED matches back to SCHEDULED
        List<SportsTournamentMatch> stageMatches = matchRepo.findByStageId(stage.getId());
        for (SportsTournamentMatch match : stageMatches) {
            if (match.getStatus() == MatchStatus.PUBLISHED) {
                match.setStatus(MatchStatus.SCHEDULED);
            }
        }
        if (!stageMatches.isEmpty()) {
            matchRepo.saveAll(stageMatches);
        }

        stage.setStageStatus(SportsTournamentStage.StageStatus.APPROVED);
        stage.setPublicationStatus(SportsTournamentStage.PublicationStatus.WITHDRAWN);
        stageRepo.save(stage);

        log.info("Unpublished stage id={} reason={}", stageId, reason);
        return toStageResponse(stage, stage.getConfig().getId());
    }

    @Transactional
    public List<PublicationResult> publishMultipleStages(Long configId, List<Long> stageIds, String publishedBy) {
        return stageIds.stream()
                .map(id -> publishStage(id, publishedBy))
                .collect(Collectors.toList());
    }

    // ─── Schedule Versions ───────────────────────────────────────────────────

    public List<VersionResponse> getScheduleVersions(Long configId, Long stageId) {
        List<SportsScheduleVersion> versions;
        if (stageId != null) {
            versions = versionRepo.findByConfigIdAndStageId(configId, stageId);
        } else {
            versions = versionRepo.findByConfigIdOrderByVersionNumberDesc(configId);
        }
        return versions.stream().map(this::toVersionResponse).collect(Collectors.toList());
    }

    public VersionResponse getScheduleVersion(Long versionId) {
        SportsScheduleVersion v = versionRepo.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule version not found: " + versionId));
        return toVersionResponse(v);
    }

    // ─── Postponement ────────────────────────────────────────────────────────

    @Transactional
    public PostponementResponse postponeMatch(Long matchId, PostponeRequest req, String requestedBy) {
        SportsTournamentMatch match = matchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + matchId));

        SportsMatchPostponement postponement = SportsMatchPostponement.builder()
                .match(match)
                .originalStartAt(match.getScheduledAt())
                .proposedStartAt(req.proposedStartAt())
                .reasonCode(req.reasonCode())
                .reasonDescription(req.reasonDescription())
                .requestedBy(requestedBy)
                .reserveDayId(req.reserveDayId())
                .build();

        SportsMatchPostponement saved = postponementRepo.save(postponement);
        match.setStatus(MatchStatus.SUSPENDED);
        matchRepo.save(match);

        log.info("Postponed match id={} reason={}", matchId, req.reasonCode());
        return toPostponementResponse(saved);
    }

    public List<PostponementResponse> getPostponementHistory(Long matchId) {
        return postponementRepo.findByMatchIdOrderByCreatedAtDesc(matchId).stream()
                .map(this::toPostponementResponse)
                .collect(Collectors.toList());
    }

    // ─── Reserve Days ────────────────────────────────────────────────────────

    public List<ReserveDayResponse> getReserveDays(Long configId) {
        return reserveDayRepo.findByConfigIdOrderByReserveDate(configId).stream()
                .map(this::toReserveDayResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReserveDayResponse createReserveDay(Long configId, ReserveDayCreateRequest req) {
        SportsTournamentConfig config = configRepo.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament config not found: " + configId));

        SportsTournamentReserveDay rd = SportsTournamentReserveDay.builder()
                .config(config)
                .reserveDate(req.reserveDate())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .reserveType(req.reserveType())
                .build();

        SportsTournamentReserveDay saved = reserveDayRepo.save(rd);
        log.info("Created reserve day id={} date={}", saved.getId(), saved.getReserveDate());
        return toReserveDayResponse(saved);
    }

    @Transactional
    public void deleteReserveDay(Long reserveDayId) {
        reserveDayRepo.deleteById(reserveDayId);
    }

    // ─── Impact Analysis ─────────────────────────────────────────────────────

    public ImpactResult getScheduleImpact(Long matchId, ImpactRequest req) {
        matchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + matchId));
        return new ImpactResult(
                new ImpactResult.ProposedChange(matchId, "scheduledAt", "current", req.newStartAt() != null ? req.newStartAt().toString() : ""),
                List.of(),
                List.of(),
                true
        );
    }

    // ─── Dashboard ───────────────────────────────────────────────────────────

    public DashboardResponse getScheduleDashboard(Long configId) {
        List<SportsTournamentMatch> matches = matchRepo.findByConfigId(configId);
        List<SportsTournamentStage> stages = stageRepo.findByConfigIdOrderByStageOrder(configId);

        long total = matches.size();
        long completed = matches.stream().filter(m -> m.getStatus() == MatchStatus.COMPLETED).count();
        long upcoming = matches.stream().filter(m -> m.getStatus() == MatchStatus.SCHEDULED || m.getStatus() == MatchStatus.PUBLISHED).count();
        long postponed = matches.stream().filter(m -> m.getStatus() == MatchStatus.SUSPENDED).count();
        long live = matches.stream().filter(m -> m.getStatus() == MatchStatus.IN_PROGRESS || m.getStatus() == MatchStatus.LIVE).count();
        long pendingPub = stages.stream().filter(s ->
                s.getStageStatus() == SportsTournamentStage.StageStatus.APPROVED).count();

        boolean hasConflicts = false;

        List<DashboardResponse.StageSummary> stageSummaries = stages.stream().map(s -> {
            long stageMatches = matches.stream().filter(m -> s.getId().equals(m.getStageId())).count();
            long stageCompleted = matches.stream()
                    .filter(m -> s.getId().equals(m.getStageId()) && m.getStatus() == MatchStatus.COMPLETED).count();
            return new DashboardResponse.StageSummary(
                    s.getId(), s.getStageName(), s.getStageType().name(),
                    s.getStageStatus().name(), s.getPublicationStatus().name(),
                    (int) stageMatches, (int) stageCompleted
            );
        }).collect(Collectors.toList());

        return new DashboardResponse(
                (int) total, (int) completed, (int) upcoming, (int) postponed,
                (int) live, (int) pendingPub, hasConflicts, stageSummaries
        );
    }

    // ─── Notifications ───────────────────────────────────────────────────────

    public NotificationResult sendStageNotification(Long stageId, String type) {
        findStageOrThrow(stageId);
        log.info("Sending notification for stage id={} type={}", stageId, type);
        return new NotificationResult(0, 0);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private SportsTournamentStage findStageOrThrow(Long stageId) {
        return stageRepo.findById(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament stage not found: " + stageId));
    }

    private StageResponse toStageResponse(SportsTournamentStage s, Long configId) {
        List<SportsTournamentMatch> matches = matchRepo.findByConfigId(configId);
        long stageMatches = matches.stream().filter(m -> s.getId().equals(m.getStageId())).count();
        long stageCompleted = matches.stream()
                .filter(m -> s.getId().equals(m.getStageId()) && m.getStatus() == MatchStatus.COMPLETED).count();

        // If stage is NOT_GENERATED, preview candidate matches count
        if (stageMatches == 0 && s.getStageStatus() == SportsTournamentStage.StageStatus.NOT_GENERATED) {
            List<MatchRound> candidateRounds = getCandidateRounds(s.getStageType());
            long candidateCount = matches.stream()
                    .filter(m -> m.getStageId() == null && (candidateRounds.contains(m.getRound()) || (s.getStageOrder() != null && s.getStageOrder().equals(m.getRoundNumber()))))
                    .count();
            if (candidateCount > 0) {
                stageMatches = candidateCount;
            }
        }

        return new StageResponse(
                s.getId(), configId, s.getStageName(),
                s.getStageType().name(), s.getStageOrder(),
                s.getStageStatus().name(), s.getSchedulingMode().name(),
                s.getPublicationStatus().name(), s.getPublishedVersionId(),
                s.getQualificationRuleId(),
                s.getStartDate() != null ? s.getStartDate().toString() : null,
                s.getEndDate() != null ? s.getEndDate().toString() : null,
                s.getCreatedAt() != null ? s.getCreatedAt().toString() : null,
                s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null,
                (int) stageMatches, (int) stageCompleted
        );
    }

    private VersionResponse toVersionResponse(SportsScheduleVersion v) {
        List<Long> changedMatchIds = List.of();
        if (v.getChangedMatchIdsJson() != null && !v.getChangedMatchIdsJson().isBlank()) {
            try {
                changedMatchIds = Arrays.stream(v.getChangedMatchIdsJson()
                        .replaceAll("[\\[\\]\\s]", "").split(","))
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
            } catch (Exception ignored) {}
        }

        List<Map<String, String>> changedFields = List.of();

        return new VersionResponse(
                v.getId(),
                v.getConfig().getId(),
                v.getStage() != null ? v.getStage().getId() : null,
                v.getVersionNumber(),
                v.getStatus().name(),
                v.getChangeReason(),
                changedMatchIds,
                changedFields,
                v.getGeneratedBy(),
                v.getApprovedBy(),
                v.getPublishedBy(),
                v.getCreatedAt() != null ? v.getCreatedAt().toString() : null,
                v.getApprovedAt() != null ? v.getApprovedAt().toString() : null,
                v.getPublishedAt() != null ? v.getPublishedAt().toString() : null
        );
    }

    private PostponementResponse toPostponementResponse(SportsMatchPostponement p) {
        return new PostponementResponse(
                p.getId(),
                p.getMatch().getId(),
                p.getOriginalStartAt() != null ? p.getOriginalStartAt().toString() : null,
                p.getProposedStartAt() != null ? p.getProposedStartAt().toString() : null,
                p.getNewStartAt() != null ? p.getNewStartAt().toString() : null,
                p.getReasonCode().name(),
                p.getReasonDescription(),
                p.getScope().name(),
                p.getRequestedBy(),
                p.getApprovedBy(),
                p.getApprovalStatus().name(),
                p.getReserveDayId(),
                p.getCreatedAt() != null ? p.getCreatedAt().toString() : null
        );
    }

    private ReserveDayResponse toReserveDayResponse(SportsTournamentReserveDay rd) {
        return new ReserveDayResponse(
                rd.getId(),
                rd.getConfig().getId(),
                rd.getStage() != null ? rd.getStage().getId() : null,
                rd.getReserveDate().toString(),
                rd.getVenue() != null ? rd.getVenue().getName() : null,
                rd.getCourt() != null ? rd.getCourt().getName() : null,
                rd.getStartTime().toString(),
                rd.getEndTime().toString(),
                rd.getReserveType().name(),
                rd.getStatus().name(),
                rd.getCreatedAt() != null ? rd.getCreatedAt().toString() : null
        );
    }

    // ─── Records (DTOs) ──────────────────────────────────────────────────────

    public record StageCreateRequest(
            String stageName,
            SportsTournamentStage.StageType stageType,
            int stageOrder,
            SportsTournamentStage.SchedulingMode schedulingMode,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate
    ) {}

    public record StageUpdateRequest(
            String stageName,
            SportsTournamentStage.StageType stageType,
            Integer stageOrder,
            SportsTournamentStage.SchedulingMode schedulingMode,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate
    ) {}

    public record StageResponse(
            Long id, Long tournamentConfigId, String stageName,
            String stageType, int stageOrder,
            String stageStatus, String schedulingMode,
            String publicationStatus, Long publishedVersionId,
            Long qualificationRuleId,
            String startDate, String endDate,
            String createdAt, String updatedAt,
            int matchCount, int completedMatchCount
    ) {}

    public record GenerateRequest(
            SportsTournamentStage.SchedulingMode schedulingMode,
            List<Long> courtIds,
            List<Integer> matchDays
    ) {}

    public record GenerateResult(int matchCount, List<String> conflicts) {}

    public record ValidateResult(boolean valid, List<String> errors, List<String> warnings) {}

    public record PublicationResult(
            Long stageId, String publishedBy, LocalDateTime publishedAt,
            String notificationStatus, int recipientCount
    ) {}

    public record VersionResponse(
            Long id, Long configId, Long stageId,
            int versionNumber, String status,
            String changeReason, List<Long> changedMatchIds,
            List<Map<String, String>> changedFields,
            String generatedBy, String approvedBy, String publishedBy,
            String createdAt, String approvedAt, String publishedAt
    ) {}

    public record PostponeRequest(
            SportsMatchPostponement.PostponementReason reasonCode,
            String reasonDescription,
            LocalDateTime proposedStartAt,
            Long reserveDayId
    ) {}

    public record PostponementResponse(
            Long id, Long matchId,
            String originalStartAt, String proposedStartAt, String newStartAt,
            String reasonCode, String reasonDescription,
            String scope, String requestedBy, String approvedBy,
            String approvalStatus, Long reserveDayId, String createdAt
    ) {}

    public record ReserveDayCreateRequest(
            java.time.LocalDate reserveDate,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime,
            SportsTournamentReserveDay.ReserveDayType reserveType
    ) {}

    public record ReserveDayResponse(
            Long id, Long configId, Long stageId,
            String reserveDate, String venueName, String courtName,
            String startTime, String endTime,
            String reserveType, String status, String createdAt
    ) {}

    public record ImpactRequest(LocalDateTime newStartAt, Long newVenueId, Long newCourtId) {}

    public record ImpactResult(
            ProposedChange proposedChange,
            List<AffectedItem> affectedItems,
            List<Conflict> conflicts,
            boolean canAutoApply
    ) {
        public record ProposedChange(Long matchId, String field, String currentValue, String proposedValue) {}
        public record AffectedItem(Long matchId, String matchName, String reason, String impactType) {}
        public record Conflict(String type, String description, String severity) {}
    }

    public record DashboardResponse(
            int totalMatches, int completedMatches, int upcomingMatches,
            int postponedMatches, int liveMatches, int pendingPublications,
            boolean hasUnresolvedConflicts,
            List<StageSummary> stages
    ) {
        public record StageSummary(
                Long id, String name, String type, String status,
                String publicationStatus, int matchCount, int completedMatchCount
        ) {}
    }

    public record NotificationResult(int sent, int failed) {}

    public record PublishMultipleRequest(List<Long> stageIds) {}
}
