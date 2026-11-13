package com.manacommunity.api.sports.service;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Post;
import com.manacommunity.api.model.PostType;
import com.manacommunity.api.repository.PostRepository;
import com.manacommunity.api.sports.dto.SportsFeedMatchResponse;
import com.manacommunity.api.sports.dto.SportsMatchResponse;
import com.manacommunity.api.sports.model.FeedPostPolicy;
import com.manacommunity.api.sports.model.MatchRound;
import com.manacommunity.api.sports.model.MatchStatus;
import com.manacommunity.api.sports.model.SportsEvent;
import com.manacommunity.api.sports.model.SportsTournament;
import com.manacommunity.api.sports.repository.SportsEventRepository;
import com.manacommunity.api.sports.repository.SportsTournamentConfigRepository;
import com.manacommunity.api.sports.repository.SportsTournamentMatchRepository;
import com.manacommunity.api.sports.repository.SportsTournamentRepository;
import com.manacommunity.api.sports.scheduler.SportsTournamentConfig;
import com.manacommunity.api.sports.scheduler.SportsTournamentMatch;
import com.manacommunity.api.sports.scheduler.SportsTournamentSchedulerService;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.service.LoggedInUserService.ResolvedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Controls which sports matches are surfaced on the community feed.
 *
 * <p>The {@code showInFeed} flag exists on tournament, event and match. {@code null} means
 * "inherit / opt-out default"; the effective value of a match uses opt-out semantics:
 * match flag → event flag → tournament flag → <b>true</b> (shown by default unless explicitly
 * suppressed). Admins set the flag to {@code false} to hide a match from the feed.
 * Live matches (and matches completed within {@link #COMPLETED_GRACE}) are returned for the
 * "Live Now" strip; once a flagged match completes, exactly one official {@code SPORTS}
 * result post is published to the feed.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SportsFeedService {

    /** How long a completed match stays in the live strip showing "Final". */
    public static final Duration COMPLETED_GRACE = Duration.ofMinutes(15);
    /** Only matches completed/scheduled within this window get a result post (avoids back-posting old results). */
    private static final Duration RESULT_POST_WINDOW = Duration.ofHours(48);

    private final SportsTournamentMatchRepository matchRepo;
    private final SportsTournamentConfigRepository configRepo;
    private final SportsTournamentRepository tournamentRepo;
    private final SportsEventRepository eventRepo;
    private final SportsTournamentSchedulerService schedulerService;
    private final PostRepository postRepo;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    // ── Getters for Feed Visibility & Policy ───────────────────────────────

    @Transactional(readOnly = true)
    public Boolean getEventFlag(Long eventId) {
        return eventRepo.findById(eventId).map(SportsEvent::getShowInFeed).orElse(null);
    }

    @Transactional(readOnly = true)
    public FeedPostPolicy getEventPolicy(Long eventId) {
        return eventRepo.findById(eventId).map(SportsEvent::getFeedPostPolicy).orElse(null);
    }

    @Transactional(readOnly = true)
    public String getEventStages(Long eventId) {
        return eventRepo.findById(eventId).map(SportsEvent::getFeedPostStages).orElse(null);
    }

    @Transactional(readOnly = true)
    public Boolean getTournamentFlag(Long tournamentId) {
        return tournamentRepo.findById(tournamentId).map(SportsTournament::getShowInFeed).orElse(null);
    }

    @Transactional(readOnly = true)
    public FeedPostPolicy getTournamentPolicy(Long tournamentId) {
        return tournamentRepo.findById(tournamentId).map(SportsTournament::getFeedPostPolicy).orElse(null);
    }

    @Transactional(readOnly = true)
    public String getTournamentStages(Long tournamentId) {
        return tournamentRepo.findById(tournamentId).map(SportsTournament::getFeedPostStages).orElse(null);
    }

    @Transactional(readOnly = true)
    public Boolean getMatchFlag(Long matchId) {
        return matchRepo.findById(matchId).map(this::isShownInFeed).orElse(null);
    }

    @Transactional(readOnly = true)
    public Boolean getMatchPostToFeed(Long matchId) {
        return matchRepo.findById(matchId).map(SportsTournamentMatch::getPostToFeed).orElse(null);
    }

    @Transactional(readOnly = true)
    public FeedPostPolicy resolveFeedPostPolicy(SportsTournamentMatch m) {
        SportsTournamentConfig cfg = m.getConfig();
        SportsEvent event = cfg != null ? cfg.getEvent() : null;
        if (event == null && cfg != null && cfg.getId() != null) {
            event = eventRepo.findById(cfg.getId()).orElse(null);
        }
        if (event != null && event.getFeedPostPolicy() != null) {
            return event.getFeedPostPolicy();
        }
        SportsTournament t = event != null ? event.getTournament() : null;
        if (t != null && t.getFeedPostPolicy() != null) {
            return t.getFeedPostPolicy();
        }
        // Default policy: FINALS_ONLY (prevents preliminary feed flooding)
        return FeedPostPolicy.FINALS_ONLY;
    }

    @Transactional(readOnly = true)
    public String resolveFeedPostStages(SportsTournamentMatch m) {
        SportsTournamentConfig cfg = m.getConfig();
        SportsEvent event = cfg != null ? cfg.getEvent() : null;
        if (event == null && cfg != null && cfg.getId() != null) {
            event = eventRepo.findById(cfg.getId()).orElse(null);
        }
        if (event != null && event.getFeedPostStages() != null && !event.getFeedPostStages().isBlank()) {
            return event.getFeedPostStages();
        }
        SportsTournament t = event != null ? event.getTournament() : null;
        if (t != null && t.getFeedPostStages() != null && !t.getFeedPostStages().isBlank()) {
            return t.getFeedPostStages();
        }
        return null;
    }

    // ── Effective flag ────────────────────────────────────────────────────

    /**
     * Returns whether this match should appear on the community feed.
     *
     * <p>Resolution order (first non-null explicit flag wins):
     * <ol>
     *   <li>Draft guard: matches under DRAFT configs or in DRAFT status are hidden
     *   <li>Match-level {@code showInFeed} override
     *   <li>Event-level {@code showInFeed} override
     *   <li>Tournament-level {@code showInFeed} override
     * </ol>
     * When <em>all</em> levels are {@code null} (no explicit opt-out), the match is shown by
     * default — this is the opt-out model so that LIVE matches appear without admins having
     * to toggle anything upfront. Admins set {@code showInFeed = false} to suppress a match.
     */
    public boolean isShownInFeed(SportsTournamentMatch m) {
        // Draft check: if parent config or match is DRAFT, don't show on community feed
        if (m.getStatus() == MatchStatus.DRAFT) return false;
        if (m.getConfig() != null && m.getConfig().getStatus() == SportsTournamentConfig.TournamentStatus.DRAFT) {
            return false;
        }

        // 1. Match-level explicit flag
        if (m.getShowInFeed() != null) return m.getShowInFeed();

        // 2. Event-level explicit flag
        SportsTournamentConfig cfg = m.getConfig();
        SportsEvent event = cfg != null ? cfg.getEvent() : null;
        if (event == null && cfg != null && cfg.getId() != null) {
            event = eventRepo.findById(cfg.getId()).orElse(null);
        }
        if (event != null && event.getShowInFeed() != null) return event.getShowInFeed();

        // 3. Tournament-level explicit flag
        //    Walk: event → tournament (normal path), or cfg community → first tournament (fallback)
        SportsTournament t = event != null ? event.getTournament() : null;
        if (t != null && t.getShowInFeed() != null) return t.getShowInFeed();

        // 4. Default: opt-out — show LIVE matches unless explicitly suppressed
        return true;
    }

    // ── Setting the flag (cascade = clear overrides below) ────────────────

    @Transactional
    public void setTournamentFlag(Long tournamentId, Boolean show, boolean cascade, ResolvedUser ctx) {
        SportsTournament t = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));
        assertCommunity(t.getCommunity(), ctx);
        t.setShowInFeed(show);
        tournamentRepo.save(t);
        if (cascade) {
            for (SportsEvent e : eventRepo.findByTournamentId(tournamentId)) {
                e.setShowInFeed(show);
                eventRepo.save(e);
                setEventFlag(e.getId(), show, true, ctx);
            }
        }
        broadcastLiveFeedUpdate();
        log.info("Feed flag: tournament {} -> {} (cascade={}) by user {}", tournamentId, show, cascade,
                ctx != null && ctx.user() != null ? ctx.user().getId() : null);
    }

    @Transactional
    public void setEventFlag(Long eventId, Boolean show, boolean cascade, ResolvedUser ctx) {
        SportsEvent e = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
        Community community = e.getCommunity() != null ? e.getCommunity()
                : (e.getTournament() != null ? e.getTournament().getCommunity() : null);
        assertCommunity(community, ctx);
        e.setShowInFeed(show);
        if (e.getCommunity() == null && community != null) {
            e.setCommunity(community);
        }
        eventRepo.save(e);
        if (cascade) {
            clearMatchOverrides(eventId, show);
        }
        broadcastLiveFeedUpdate();
        log.info("Feed flag: event {} -> {} (cascade={}) by user {}", eventId, show, cascade,
                ctx != null && ctx.user() != null ? ctx.user().getId() : null);
    }

    @Transactional
    public void setMatchFlag(Long matchId, Boolean show, ResolvedUser ctx) {
        SportsTournamentMatch m = matchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", matchId));
        Community community = m.getCommunity();
        if (community == null && m.getConfig() != null) {
            community = m.getConfig().getCommunity();
            if (community == null && m.getConfig().getEvent() != null) {
                community = m.getConfig().getEvent().getCommunity();
                if (community == null && m.getConfig().getEvent().getTournament() != null) {
                    community = m.getConfig().getEvent().getTournament().getCommunity();
                }
            }
        }
        assertCommunity(community, ctx);
        m.setShowInFeed(show);
        matchRepo.save(m);
        broadcastLiveFeedUpdate();
        log.info("Feed flag: match {} -> {} by user {}", matchId, show,
                ctx != null && ctx.user() != null ? ctx.user().getId() : null);
    }

    @Transactional
    public void setTournamentSettings(Long tournamentId, Boolean showInLiveStrip, FeedPostPolicy policy, String stages, boolean cascade, ResolvedUser ctx) {
        SportsTournament t = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));
        assertCommunity(t.getCommunity(), ctx);
        if (showInLiveStrip != null) t.setShowInFeed(showInLiveStrip);
        if (policy != null) t.setFeedPostPolicy(policy);
        if (stages != null) t.setFeedPostStages(stages);
        tournamentRepo.save(t);
        if (cascade) {
            for (SportsEvent e : eventRepo.findByTournamentId(tournamentId)) {
                if (showInLiveStrip != null) e.setShowInFeed(showInLiveStrip);
                if (policy != null) e.setFeedPostPolicy(policy);
                if (stages != null) e.setFeedPostStages(stages);
                eventRepo.save(e);
                setEventSettings(e.getId(), showInLiveStrip, policy, stages, true, ctx);
            }
        }
        broadcastLiveFeedUpdate();
        log.info("Feed settings: tournament {} -> live={}, policy={}, stages={} (cascade={}) by user {}",
                tournamentId, showInLiveStrip, policy, stages, cascade,
                ctx != null && ctx.user() != null ? ctx.user().getId() : null);
    }

    @Transactional
    public void setEventSettings(Long eventId, Boolean showInLiveStrip, FeedPostPolicy policy, String stages, boolean cascade, ResolvedUser ctx) {
        SportsEvent e = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
        Community community = e.getCommunity() != null ? e.getCommunity()
                : (e.getTournament() != null ? e.getTournament().getCommunity() : null);
        assertCommunity(community, ctx);
        if (showInLiveStrip != null) e.setShowInFeed(showInLiveStrip);
        if (policy != null) e.setFeedPostPolicy(policy);
        if (stages != null) e.setFeedPostStages(stages);
        if (e.getCommunity() == null && community != null) {
            e.setCommunity(community);
        }
        eventRepo.save(e);
        if (cascade && showInLiveStrip != null) {
            clearMatchOverrides(eventId, showInLiveStrip);
        }
        broadcastLiveFeedUpdate();
        log.info("Feed settings: event {} -> live={}, policy={}, stages={} (cascade={}) by user {}",
                eventId, showInLiveStrip, policy, stages, cascade,
                ctx != null && ctx.user() != null ? ctx.user().getId() : null);
    }

    @Transactional
    public void setMatchSettings(Long matchId, Boolean showInLiveStrip, Boolean postToFeed, ResolvedUser ctx) {
        SportsTournamentMatch m = matchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", matchId));
        Community community = m.getCommunity();
        if (community == null && m.getConfig() != null) {
            community = m.getConfig().getCommunity();
            if (community == null && m.getConfig().getEvent() != null) {
                community = m.getConfig().getEvent().getCommunity();
                if (community == null && m.getConfig().getEvent().getTournament() != null) {
                    community = m.getConfig().getEvent().getTournament().getCommunity();
                }
            }
        }
        assertCommunity(community, ctx);
        if (showInLiveStrip != null) m.setShowInFeed(showInLiveStrip);
        if (postToFeed != null) m.setPostToFeed(postToFeed);
        matchRepo.save(m);
        broadcastLiveFeedUpdate();
        log.info("Feed settings: match {} -> live={}, postToFeed={} by user {}",
                matchId, showInLiveStrip, postToFeed,
                ctx != null && ctx.user() != null ? ctx.user().getId() : null);
    }

    private void clearMatchOverrides(Long eventId, Boolean show) {
        List<SportsTournamentConfig> configs = new java.util.ArrayList<>(configRepo.findByEventId(eventId));
        configRepo.findById(eventId).ifPresent(c -> {
            if (!configs.contains(c)) configs.add(c);
        });
        for (SportsTournamentConfig cfg : configs) {
            List<SportsTournamentMatch> matches = matchRepo.findByConfigId(cfg.getId());
            matches.forEach(mm -> mm.setShowInFeed(show));
            matchRepo.saveAll(matches);
        }
    }

    private void assertCommunity(Community community, ResolvedUser ctx) {
        if (ctx == null || ctx.superAdmin()) return;
        if (community == null) return;
        if (ctx.communityId() != null && !Objects.equals(community.getId(), ctx.communityId())) {
            if (ctx.user() == null || (!ctx.user().hasRole(com.manacommunity.api.constants.PermissionConstants.ROLE_SUPER_ADMIN)
                    && !ctx.user().hasRole(com.manacommunity.api.constants.PermissionConstants.ROLE_ADMIN)
                    && !ctx.user().hasRole(com.manacommunity.api.constants.PermissionConstants.ROLE_SPORTS_ADMIN))) {
                throw new AccessDeniedException("Not allowed to manage this community's sports feed visibility");
            }
        }
    }

    public void broadcastLiveFeedUpdate() {
        try {
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/sports/live", java.util.Map.of("type", "REFRESH"));
            }
        } catch (Exception ex) {
            log.debug("Could not broadcast live feed update: {}", ex.getMessage());
        }
    }

    // ── Live strip query ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SportsFeedMatchResponse> getLiveFeed(Long communityIdParam, ResolvedUser ctx) {
        Long targetCommunityId = communityIdParam != null
                ? communityIdParam
                : (ctx != null ? ctx.communityId() : null);
        LocalDateTime since = LocalDateTime.now().minus(COMPLETED_GRACE);

        List<SportsTournamentMatch> candidates;
        if (targetCommunityId != null) {
            candidates = matchRepo.findFeedCandidatesByCommunityId(targetCommunityId, since);
            // Fallback: If community-scoped candidates are empty, load candidates across communities
            if (candidates.isEmpty()) {
                candidates = matchRepo.findFeedCandidatesAll(since);
            }
        } else {
            candidates = matchRepo.findFeedCandidatesAll(since);
        }

        return candidates.stream()
                .filter(this::isShownInFeed)
                .map(this::toFeedResponse)
                .toList();
    }

    private SportsFeedMatchResponse toFeedResponse(SportsTournamentMatch m) {
        SportsMatchResponse r = schedulerService.toMatchResponse(m);
        SportsTournamentConfig cfg = m.getConfig();
        SportsEvent event = cfg != null ? cfg.getEvent() : null;
        if (event == null && cfg != null && cfg.getId() != null) {
            event = eventRepo.findById(cfg.getId()).orElse(null);
        }
        SportsTournament t = event != null ? event.getTournament() : null;

        String sportName = cfg != null && cfg.getSport() != null ? cfg.getSport().getName()
                : (event != null && event.getSport() != null ? event.getSport().getName() : "Cricket");

        String tournamentName = t != null ? t.getName()
                : (cfg != null && cfg.getTournamentName() != null ? cfg.getTournamentName()
                : (event != null && event.getTournament() != null ? event.getTournament().getName() : null));

        String eventName = event != null ? event.getName()
                : (cfg != null ? cfg.getTournamentName() : null);

        String status = ("IN_PROGRESS".equalsIgnoreCase(r.status()) || "LIVE".equalsIgnoreCase(r.status()))
                ? "LIVE" : r.status();

        return new SportsFeedMatchResponse(
                m.getId(),
                cfg != null ? cfg.getId() : null,
                sportName,
                tournamentName,
                eventName,
                r.roundName(),
                r.teamAName(), r.teamBName(),
                r.teamAFlat(), r.teamBFlat(),
                r.scoreTeamA(), r.scoreTeamB(),
                status, r.winnerName(),
                r.scheduledAt(),
                m.getCompletedAt() != null ? m.getCompletedAt().toString() : null,
                r.venueName());
    }

    // ── Result post on completion ─────────────────────────────────────────

    /**
     * Determines whether a completed match should automatically publish an official
     * result post into the community feed based on its stage and configured policy.
     */
    public boolean shouldPublishResultPost(SportsTournamentMatch m) {
        if (m == null || m.getStatus() != MatchStatus.COMPLETED) return false;

        // 1. Explicit match override wins if present
        if (Boolean.FALSE.equals(m.getPostToFeed())) return false;
        if (Boolean.TRUE.equals(m.getPostToFeed())) return true;

        // 2. Draft configs or matches should never publish to feed
        if (m.getConfig() != null && m.getConfig().getStatus() == SportsTournamentConfig.TournamentStatus.DRAFT) {
            return false;
        }

        // 3. Resolve policy from Event -> Tournament -> Default (FINALS_ONLY)
        FeedPostPolicy policy = resolveFeedPostPolicy(m);
        if (policy == FeedPostPolicy.NONE) return false;
        if (policy == FeedPostPolicy.ALL_MATCHES) return true;

        MatchRound r = m.getRound();
        String round = (r != null ? r.name() : "").toUpperCase().trim();

        boolean isFinal = (r == MatchRound.FINAL || r == MatchRound.GRAND_FINAL || r == MatchRound.SUPER_FINAL
                || round.contains("FINAL"))
                && !round.contains("SEMI")
                && !round.contains("QUARTER")
                && !round.contains("1/8")
                && !round.contains("ROUND_OF");

        if (policy == FeedPostPolicy.FINALS_ONLY) {
            return isFinal;
        }

        if (policy == FeedPostPolicy.KNOCKOUTS_ONLY) {
            boolean isKnockout = isFinal
                    || r == MatchRound.SEMI_FINAL
                    || r == MatchRound.QUARTER_FINAL
                    || r == MatchRound.THIRD_PLACE
                    || r == MatchRound.ROUND_OF_16
                    || r == MatchRound.ROUND_OF_32
                    || r == MatchRound.ROUND_OF_64
                    || r == MatchRound.ELIMINATOR
                    || r == MatchRound.QUALIFIER_1
                    || r == MatchRound.QUALIFIER_2
                    || round.contains("SEMI") || round.contains("QUARTER")
                    || round.contains("THIRD") || round.contains("3RD")
                    || round.contains("KNOCKOUT") || round.contains("PLAYOFF");
            return isKnockout;
        }

        if (policy == FeedPostPolicy.CUSTOM) {
            String stagesConfig = resolveFeedPostStages(m);
            if (stagesConfig == null || stagesConfig.isBlank()) return false;
            for (String allowed : stagesConfig.split(",")) {
                String a = allowed.trim().toUpperCase();
                if (!a.isEmpty() && round.contains(a)) {
                    return true;
                }
            }
            return false;
        }

        return false;
    }

    /**
     * Publishes one official SPORTS result post for each completed match that satisfies
     * the feed post policy (or has explicit postToFeed=true).
     * Polling keeps this decoupled from various score-recording flows, while feedResultPostId
     * guarantees exactly-once publication.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void publishPendingResultPosts() {
        LocalDateTime since = LocalDateTime.now().minus(RESULT_POST_WINDOW);
        for (SportsTournamentMatch m : matchRepo.findCompletedWithoutFeedPost(since)) {
            try {
                if (!shouldPublishResultPost(m)) continue;
                Long postId = createResultPost(m);
                if (postId != null) {
                    m.setFeedResultPostId(postId);
                    matchRepo.save(m);
                }
            } catch (Exception e) {
                log.error("Failed to publish feed result post for match {}", m.getId(), e);
            }
        }
    }

    /**
     * Allows tournament organizers and admins to manually publish a completed match result
     * to the community feed on-demand (e.g. for exciting group matches or special highlights).
     */
    @Transactional
    public Long publishMatchResultPost(Long matchId, ResolvedUser ctx) {
        SportsTournamentMatch m = matchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", matchId));
        if (m.getStatus() != MatchStatus.COMPLETED) {
            throw new IllegalStateException("Match #" + matchId + " is not completed yet");
        }
        if (m.getFeedResultPostId() != null) {
            return m.getFeedResultPostId();
        }
        Long postId = createResultPost(m);
        if (postId != null) {
            m.setFeedResultPostId(postId);
            m.setPostToFeed(true);
            matchRepo.save(m);
            log.info("Manually published feed result post {} for match {} by user {}",
                    postId, matchId, ctx != null && ctx.user() != null ? ctx.user().getId() : null);
        }
        return postId;
    }

    private Long createResultPost(SportsTournamentMatch m) {
        SportsTournamentConfig cfg = m.getConfig();
        Community community = cfg != null && cfg.getCommunity() != null ? cfg.getCommunity() : m.getCommunity();
        AppUser author = cfg != null ? cfg.getCreatedBy() : null;
        if (author == null && cfg != null && cfg.getEvent() != null && cfg.getEvent().getTournament() != null) {
            author = cfg.getEvent().getTournament().getCreatedBy();
        }
        if (author == null && cfg != null && cfg.getEvent() != null) author = cfg.getEvent().getCreatedBy();
        if (community == null || author == null) {
            log.warn("Skipping feed result post for match {}: missing community or author", m.getId());
            return null;
        }

        SportsMatchResponse r = schedulerService.toMatchResponse(m);
        String scoreA = r.scoreTeamA() != null ? r.scoreTeamA() : "-";
        String scoreB = r.scoreTeamB() != null ? r.scoreTeamB() : "-";
        String context = joinNonBlank(" · ",
                cfg != null && cfg.getEvent() != null && cfg.getEvent().getTournament() != null
                        ? cfg.getEvent().getTournament().getName() : null,
                cfg != null && cfg.getEvent() != null ? cfg.getEvent().getName() : null,
                r.roundName());

        StringBuilder body = new StringBuilder();
        body.append("🏆 Final: ").append(r.teamAName()).append(" ").append(scoreA)
            .append(" vs ").append(r.teamBName()).append(" ").append(scoreB).append("\n");
        if (r.winnerName() != null && !r.winnerName().isBlank()) {
            body.append("Winner: ").append(r.winnerName()).append("\n");
        }
        if (m.getMatchSummary() != null && !m.getMatchSummary().isBlank()) {
            body.append(m.getMatchSummary()).append("\n");
        }
        if (!context.isBlank()) body.append(context);

        Post post = Post.builder()
                .user(author)
                .community(community)
                .postType(PostType.SPORTS)
                .official(true)
                .title("Match result: " + r.teamAName() + " vs " + r.teamBName())
                .content(body.toString().trim())
                .build();
        return postRepo.save(post).getId();
    }

    private static String joinNonBlank(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank() || "TBD".equals(p)) continue;
            if (sb.length() > 0) sb.append(sep);
            sb.append(p);
        }
        return sb.toString();
    }
}
