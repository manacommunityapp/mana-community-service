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

/**
 * Feed visibility for sports matches. Toggling is available to Sports Admins,
 * Community Admins, Admins, Super Admins, and users with CREATE_EDIT_SPORTS_MAIN.
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

    /** GET visibility status of an event. */
    @GetMapping("/events/{id}/feed-visibility")
    public ResponseEntity<Map<String, Object>> getEventVisibility(@PathVariable Long id) {
        Boolean visible = feedService.getEventFlag(id);
        return ResponseEntity.ok(Map.of(
                "eventId", id,
                "showInFeed", visible != null ? visible : true,
                "explicit", visible != null));
    }

    /** GET visibility status of a tournament. */
    @GetMapping("/tournaments/{id}/feed-visibility")
    public ResponseEntity<Map<String, Object>> getTournamentVisibility(@PathVariable Long id) {
        Boolean visible = feedService.getTournamentFlag(id);
        return ResponseEntity.ok(Map.of(
                "tournamentId", id,
                "showInFeed", visible != null ? visible : true,
                "explicit", visible != null));
    }

    /** GET visibility status of a match. */
    @GetMapping("/matches/{id}/feed-visibility")
    public ResponseEntity<Map<String, Object>> getMatchVisibility(@PathVariable Long id) {
        Boolean visible = feedService.getMatchFlag(id);
        return ResponseEntity.ok(Map.of(
                "matchId", id,
                "showInFeed", visible != null ? visible : true,
                "explicit", visible != null));
    }

    /** Body: {@code {"showInFeed": true|false|null, "cascade": true|false}}. */
    @PutMapping("/tournaments/{id}/feed-visibility")
    public ResponseEntity<Void> setTournament(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        requireSportsAdminOrPermission(principal);
        feedService.setTournamentFlag(id, flag(body), cascade(body), loggedInUserService.resolveContext(principal));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/events/{id}/feed-visibility")
    public ResponseEntity<Void> setEvent(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        requireSportsAdminOrPermission(principal);
        feedService.setEventFlag(id, flag(body), cascade(body), loggedInUserService.resolveContext(principal));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/matches/{id}/feed-visibility")
    public ResponseEntity<Void> setMatch(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        requireSportsAdminOrPermission(principal);
        feedService.setMatchFlag(id, flag(body), loggedInUserService.resolveContext(principal));
        return ResponseEntity.noContent().build();
    }

    /** Live matches (and ones completed in the last 15 min) flagged for the community feed. */
    @GetMapping("/live-feed")
    public ResponseEntity<List<SportsFeedMatchResponse>> liveFeed(
            @RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        ResolvedUser ctx = principal != null ? loggedInUserService.resolveContext(principal) : null;
        return ResponseEntity.ok(feedService.getLiveFeed(communityId, ctx));
    }

    private static Boolean flag(Map<String, Object> body) {
        Object v = body.get("showInFeed");
        return v == null ? null : Boolean.valueOf(String.valueOf(v));
    }

    private static boolean cascade(Map<String, Object> body) {
        return Boolean.parseBoolean(String.valueOf(body.getOrDefault("cascade", false)));
    }
}
