package com.manacommunity.api.sports.service;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Post;
import com.manacommunity.api.model.PostType;
import com.manacommunity.api.repository.PostRepository;
import com.manacommunity.api.sports.dto.SportsFeedMatchResponse;
import com.manacommunity.api.sports.dto.SportsMatchResponse;
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

    // ── Effective flag ────────────────────────────────────────────────────

    /**
     * Returns whether this match should appear on the community feed.
     *
     * <p>Resolution order (first non-null explicit flag wins):
     * <ol>
     *   <li>Match-level {@code showInFeed} override
     *   <li>Event-level {@code showInFeed} override
     *   <li>Tournament-level {@code showInFeed} override
     * </ol>
     * When <em>all</em> levels are {@code null} (no explicit opt-out), the match is shown by
     * default — this is the opt-out model so that LIVE matches appear without admins having
     * to toggle anything upfront. Admins set {@code showInFeed = false} to suppress a match.
     */
    public boolean isShownInFeed(SportsTournamentMatch m) {
        // 1. Match-level explicit flag
        if (m.getShowInFeed() != null) return m.getShowInFeed();

        // 2. Event-level explicit flag
        SportsTournamentConfig cfg = m.getConfig();
        SportsEvent event = cfg != null ? cfg.getEvent() : null;
        if (event != null && event.getShowInFeed() != null) return event.getShowInFeed();

        // 3. Tournament-level explicit flag
        //    Walk: event → tournament (normal path), or cfg community → first tournament (fallback)
        SportsTournament t = event != null ? event.getTournament() : null;
        if (t == null && cfg != null && cfg.getCommunity() != null) {
            // Config is not linked to a SportsEvent (standalone schedule);
            // look up tournament by community — matches without an event linkage inherit community default.
            // No tournament found on this path → still apply opt-out default (show).
        }
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
                e.setShowInFeed(null);
                eventRepo.save(e);
                clearMatchOverrides(e.getId());
            }
        }
        log.info("Feed flag: tournament {} -> {} (cascade={}) by user {}", tournamentId, show, cascade,
                ctx.user() != null ? ctx.user().getId() : null);
    }

    @Transactional
    public void setEventFlag(Long eventId, Boolean show, boolean cascade, ResolvedUser ctx) {
        SportsEvent e = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
        assertCommunity(e.getCommunity(), ctx);
        e.setShowInFeed(show);
        eventRepo.save(e);
        if (cascade) clearMatchOverrides(eventId);
        log.info("Feed flag: event {} -> {} (cascade={}) by user {}", eventId, show, cascade,
                ctx.user() != null ? ctx.user().getId() : null);
    }

    @Transactional
    public void setMatchFlag(Long matchId, Boolean show, ResolvedUser ctx) {
        SportsTournamentMatch m = matchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", matchId));
        assertCommunity(m.getConfig() != null ? m.getConfig().getCommunity() : m.getCommunity(), ctx);
        m.setShowInFeed(show);
        matchRepo.save(m);
        log.info("Feed flag: match {} -> {} by user {}", matchId, show,
                ctx.user() != null ? ctx.user().getId() : null);
    }

    private void clearMatchOverrides(Long eventId) {
        for (SportsTournamentConfig cfg : configRepo.findByEventId(eventId)) {
            List<SportsTournamentMatch> matches = matchRepo.findByConfigId(cfg.getId());
            matches.forEach(mm -> mm.setShowInFeed(null));
            matchRepo.saveAll(matches);
        }
    }

    private void assertCommunity(Community community, ResolvedUser ctx) {
        if (ctx.superAdmin()) return;
        if (community == null || ctx.communityId() == null
                || !Objects.equals(community.getId(), ctx.communityId())) {
            throw new AccessDeniedException("Not allowed to manage this community's sports feed visibility");
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
        } else if (ctx != null && ctx.superAdmin()) {
            candidates = matchRepo.findFeedCandidatesAll(since);
        } else {
            return List.of();
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
        SportsTournament t = event != null ? event.getTournament() : null;
        return new SportsFeedMatchResponse(
                m.getId(),
                cfg != null ? cfg.getId() : null,
                cfg != null && cfg.getSport() != null ? cfg.getSport().getName() : null,
                t != null ? t.getName() : (cfg != null ? cfg.getTournamentName() : null),
                event != null ? event.getName() : null,
                r.roundName(),
                r.teamAName(), r.teamBName(),
                r.teamAFlat(), r.teamBFlat(),
                r.scoreTeamA(), r.scoreTeamB(),
                r.status(), r.winnerName(),
                r.scheduledAt(),
                m.getCompletedAt() != null ? m.getCompletedAt().toString() : null,
                r.venueName());
    }

    // ── Result post on completion ─────────────────────────────────────────

    /**
     * Publishes one official SPORTS result post for each completed, feed-enabled match.
     * Polling (rather than hooking each completion path) keeps this independent of the
     * several services that can complete a match, and {@code feedResultPostId} guarantees idempotence.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void publishPendingResultPosts() {
        LocalDateTime since = LocalDateTime.now().minus(RESULT_POST_WINDOW);
        for (SportsTournamentMatch m : matchRepo.findCompletedWithoutFeedPost(since)) {
            try {
                if (m.getStatus() != MatchStatus.COMPLETED || !isShownInFeed(m)) continue;
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
