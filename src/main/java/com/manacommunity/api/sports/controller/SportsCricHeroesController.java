package com.manacommunity.api.sports.controller;

import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.service.SportsCricHeroesService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.manacommunity.api.constants.permissions.SportsPermissions.*;

@Slf4j
@RestController
@RequestMapping("/api/cricheroes")
@RequiredArgsConstructor
public class SportsCricHeroesController {

    private final SportsCricHeroesService cricHeroesService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.manacommunity.platform.sqs.SqsMessagePublisher sqsMessagePublisher;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.manacommunity.platform.sqs.SqsProperties sqsProperties;

    @PostMapping("/link")
    public ResponseEntity<SportsCricHeroesLinkResponse> linkProfile(
            @Valid @RequestBody SportsCricHeroesLinkRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_PLAYER_POOL, CREATE_EDIT_AUCTION_CONFIG);
        AppUser user = loggedInUserService.resolve(principal);
        log.info("Linking CricHeroes profile for player {}", request.getPlayerId());
        SportsCricHeroesLinkResponse response = cricHeroesService.linkProfile(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/link/{playerId}")
    public ResponseEntity<Void> unlinkProfile(
            @PathVariable Long playerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_PLAYER_POOL, CREATE_EDIT_AUCTION_CONFIG);
        AppUser user = loggedInUserService.resolve(principal);
        log.info("Unlinking CricHeroes profile from player {}", playerId);
        cricHeroesService.unlinkProfile(playerId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profile/{playerId}")
    public ResponseEntity<SportsCricHeroesProfileResponse> getProfile(
            @PathVariable Long playerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_PLAYER_POOL, VIEW_LIVE_AUCTION);
        return ResponseEntity.ok(cricHeroesService.getProfile(playerId));
    }

    @GetMapping("/profile/{playerId}/refresh")
    public ResponseEntity<SportsCricHeroesProfileResponse> refreshProfile(
            @PathVariable Long playerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_PLAYER_POOL, CREATE_EDIT_AUCTION_CONFIG);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(cricHeroesService.refreshProfile(playerId, user));
    }

    /**
     * Async CricHeroes refresh queued via SQS ({@code mana-external-sync}) so web threads
     * never block on external HTML scraping.
     */
    @PostMapping("/profile/{playerId}/refresh-async")
    public ResponseEntity<Map<String, Object>> refreshProfileAsync(
            @PathVariable Long playerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_PLAYER_POOL, CREATE_EDIT_AUCTION_CONFIG);
        AppUser user = loggedInUserService.resolve(principal);
        if (sqsMessagePublisher != null && sqsProperties != null) {
            sqsMessagePublisher.publish(
                    sqsProperties.getQueues().getExternalSync(),
                    "CRICHEROES_REFRESH",
                    Map.of("playerId", playerId, "userId", user.getId()),
                    () -> cricHeroesService.refreshProfile(playerId, user)
            );
        } else {
            cricHeroesService.refreshProfile(playerId, user);
        }
        return ResponseEntity.accepted().body(Map.of(
                "status", "QUEUED",
                "playerId", playerId
        ));
    }

    @GetMapping("/preview")
    public ResponseEntity<SportsCricHeroesProfileResponse> previewProfile(
            @RequestParam String url,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_PLAYER_POOL, CREATE_EDIT_PLAYER_POOL);
        return ResponseEntity.ok(cricHeroesService.previewProfile(url));
    }

    @GetMapping("/rating/{playerId}")
    public ResponseEntity<SportsPlayerRatingResponse> getPlayerRating(
            @PathVariable Long playerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_PLAYER_POOL, VIEW_LIVE_AUCTION);
        return ResponseEntity.ok(cricHeroesService.getPlayerRating(playerId));
    }

    @GetMapping("/profiles/{configId}")
    public ResponseEntity<Map<Long, SportsCricHeroesProfileResponse>> getLinkedProfiles(
            @PathVariable Long configId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_PLAYER_POOL, VIEW_LIVE_AUCTION);
        return ResponseEntity.ok(cricHeroesService.getLinkedProfiles(configId));
    }

    @GetMapping("/team-composition/{configId}/{teamId}")
    public ResponseEntity<SportsTeamCompositionResponse> getTeamComposition(
            @PathVariable Long configId,
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_TEAMS_DASHBOARD, VIEW_LIVE_AUCTION);
        return ResponseEntity.ok(cricHeroesService.getTeamComposition(configId, teamId));
    }
}
