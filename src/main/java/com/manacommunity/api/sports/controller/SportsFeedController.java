package com.manacommunity.api.sports.controller;

import com.manacommunity.api.sports.dto.SportsFeedMatchResponse;
import com.manacommunity.api.sports.service.SportsFeedService;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.user.service.LoggedInUserService.ResolvedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import com.manacommunity.api.user.model.AppUser;
import static com.manacommunity.api.constants.PermissionConstants.ROLE_ADMIN;
import static com.manacommunity.api.constants.PermissionConstants.ROLE_COMMUNITY_ADMIN;
import static com.manacommunity.api.constants.PermissionConstants.ROLE_SPORTS_ADMIN;
import static com.manacommunity.api.constants.PermissionConstants.ROLE_SUPER_ADMIN;
import static com.manacommunity.api.constants.permissions.SportsPermissions.CREATE_EDIT_SPORTS_MAIN;
import static com.manacommunity.api.constants.permissions.SportsPermissions.VIEW_SPORTS_MAIN;

import com.manacommunity.api.sports.model.FeedPostPolicy;
import java.util.HashMap;

/**
 * Feed visibility and result posting policies for sports matches.
 * Toggling is available to Sports Admins, Community Admins, Admins, Super Admins,
 * and users with CREATE_EDIT_SPORTS_MAIN.
 */
@RestController
@RequestMapping("/api/sports")
@RequiredArgsConstructor
public class SportsFeedController {

    private final SportsFeedService feedService;
    private final LoggedInUserService loggedInUserService;
    private final com.manacommunity.api.service.PermissionCheckService permissionCheckService;

    private void requireSportsAdminOrPermission(UserPrincipal principal) {
        if (principal == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }
        AppUser user = loggedInUserService.resolve(principal);
        if (user != null && (user.hasRole(ROLE_SUPER_ADMIN)
                || user.hasRole(ROLE_ADMIN)
                || user.hasRole(ROLE_COMMUNITY_ADMIN)
                || user.hasRole(ROLE_SPORTS_ADMIN))) {
            return;
        }
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
    }

    /** GET visibility status and feed post policy of an event. */
    @GetMapping("/events/{id}/feed-visibility")
    public ResponseEntity<Map<String, Object>> getEventVisibility(@PathVariable Long id) {
        Boolean visible = feedService.getEventFlag(id);
        FeedPostPolicy policy = feedService.getEventPolicy(id);
        String stages = feedService.getEventStages(id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("eventId", id);
        resp.put("showInFeed", visible != null ? visible : true);
        resp.put("explicit", visible != null);
        resp.put("feedPostPolicy", policy != null ? policy.name() : "FINALS_ONLY");
        resp.put("feedPostStages", stages != null ? stages : "");
        return ResponseEntity.ok(resp);
    }

    /** GET visibility status and feed post policy of a tournament. */
    @GetMapping("/tournaments/{id}/feed-visibility")
    public ResponseEntity<Map<String, Object>> getTournamentVisibility(@PathVariable Long id) {
        Boolean visible = feedService.getTournamentFlag(id);
        FeedPostPolicy policy = feedService.getTournamentPolicy(id);
        String stages = feedService.getTournamentStages(id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("tournamentId", id);
        resp.put("showInFeed", visible != null ? visible : true);
        resp.put("explicit", visible != null);
        resp.put("feedPostPolicy", policy != null ? policy.name() : "FINALS_ONLY");
        resp.put("feedPostStages", stages != null ? stages : "");
        return ResponseEntity.ok(resp);
    }

    /** GET visibility status of a match. */
    @GetMapping("/matches/{id}/feed-visibility")
    public ResponseEntity<Map<String, Object>> getMatchVisibility(@PathVariable Long id) {
        Boolean visible = feedService.getMatchFlag(id);
        Boolean postToFeed = feedService.getMatchPostToFeed(id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("matchId", id);
        resp.put("showInFeed", visible != null ? visible : true);
        resp.put("explicit", visible != null);
        resp.put("postToFeed", postToFeed);
        return ResponseEntity.ok(resp);
    }

    /**
     * Body: {@code {"showInFeed": true|false|null, "feedPostPolicy": "FINALS_ONLY"|..., "feedPostStages": "...", "cascade": true|false}}.
     */
    @PutMapping("/tournaments/{id}/feed-visibility")
    public ResponseEntity<Void> setTournament(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        requireSportsAdminOrPermission(principal);
        feedService.setTournamentSettings(
                id,
                flag(body, "showInFeed"),
                parsePolicy(body.get("feedPostPolicy")),
                stringOrNull(body.get("feedPostStages")),
                cascade(body),
                loggedInUserService.resolveContext(principal));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/events/{id}/feed-visibility")
    public ResponseEntity<Void> setEvent(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        requireSportsAdminOrPermission(principal);
        feedService.setEventSettings(
                id,
                flag(body, "showInFeed"),
                parsePolicy(body.get("feedPostPolicy")),
                stringOrNull(body.get("feedPostStages")),
                cascade(body),
                loggedInUserService.resolveContext(principal));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/matches/{id}/feed-visibility")
    public ResponseEntity<Void> setMatch(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        requireSportsAdminOrPermission(principal);
        feedService.setMatchSettings(
                id,
                flag(body, "showInFeed"),
                flag(body, "postToFeed"),
                loggedInUserService.resolveContext(principal));
        return ResponseEntity.noContent().build();
    }

    /** Manual trigger to publish a completed match result to the community feed. */
    @PostMapping("/matches/{id}/publish-result-post")
    public ResponseEntity<Map<String, Object>> publishResultPost(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        requireSportsAdminOrPermission(principal);
        Long postId = feedService.publishMatchResultPost(id, loggedInUserService.resolveContext(principal));
        return ResponseEntity.ok(Map.of("matchId", id, "postId", postId, "success", true));
    }

    /** Live matches (and ones completed in the last 15 min) flagged for the community feed. */
    @GetMapping("/live-feed")
    public ResponseEntity<List<SportsFeedMatchResponse>> liveFeed(
            @RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        ResolvedUser ctx = principal != null ? loggedInUserService.resolveContext(principal) : null;
        return ResponseEntity.ok(feedService.getLiveFeed(communityId, ctx));
    }

    private static Boolean flag(Map<String, Object> body, String key) {
        if (!body.containsKey(key)) return null;
        Object v = body.get(key);
        return v == null ? null : Boolean.valueOf(String.valueOf(v));
    }

    private static FeedPostPolicy parsePolicy(Object v) {
        if (v == null) return null;
        try {
            return FeedPostPolicy.valueOf(String.valueOf(v).trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String stringOrNull(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static boolean cascade(Map<String, Object> body) {
        return Boolean.parseBoolean(String.valueOf(body.getOrDefault("cascade", false)));
    }
}
