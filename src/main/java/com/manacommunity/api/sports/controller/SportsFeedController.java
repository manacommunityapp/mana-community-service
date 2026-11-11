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

import static com.manacommunity.api.constants.permissions.SportsPermissions.CREATE_EDIT_SPORTS_MAIN;

/**
 * Feed visibility for sports matches. Toggling is limited to users holding the
 * sports create/edit permission (sports admin / community admin) and is scoped to
 * the caller's community; reading the live strip only needs an authenticated member.
 */
@RestController
@RequestMapping("/api/sports")
@RequiredArgsConstructor
public class SportsFeedController {

    private final SportsFeedService feedService;
    private final LoggedInUserService loggedInUserService;
    private final com.manacommunity.api.service.PermissionCheckService permissionCheckService;

    /** Body: {@code {"showInFeed": true|false|null, "cascade": true|false}}. */
    @PutMapping("/tournaments/{id}/feed-visibility")
    public ResponseEntity<Void> setTournament(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        feedService.setTournamentFlag(id, flag(body), cascade(body), loggedInUserService.resolveContext(principal));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/events/{id}/feed-visibility")
    public ResponseEntity<Void> setEvent(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        feedService.setEventFlag(id, flag(body), cascade(body), loggedInUserService.resolveContext(principal));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/matches/{id}/feed-visibility")
    public ResponseEntity<Void> setMatch(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
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
