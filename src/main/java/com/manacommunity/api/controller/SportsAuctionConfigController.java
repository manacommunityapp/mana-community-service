package com.manacommunity.api.controller;

import com.manacommunity.api.dto.SportsAuctionConfigRequest;
import com.manacommunity.api.dto.SportsAuctionConfigResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.SportsAuctionConfig;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.service.SportsAuctionCsvService;
import com.manacommunity.api.service.SportsAuctionService;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.service.PermissionCheckService;
import static com.manacommunity.api.constants.PermissionConstants.*;
import static com.manacommunity.api.constants.permissions.SportsPermissions.*;
import static com.manacommunity.api.constants.permissions.SportsPermissions.CREATE_EDIT_AUCTION_CONFIG;
import static com.manacommunity.api.constants.permissions.SportsPermissions.CREATE_EDIT_LIVE_AUCTION;
import static com.manacommunity.api.constants.permissions.SportsPermissions.CREATE_EDIT_PLAYER_POOL;
import static com.manacommunity.api.constants.permissions.SportsPermissions.VIEW_AUCTION_CONFIG;
import static com.manacommunity.api.constants.permissions.SportsPermissions.VIEW_LIVE_AUCTION;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auction/config")
@RequiredArgsConstructor
public class SportsAuctionConfigController {

    private final SportsAuctionService     auctionService;
    private final SportsAuctionCsvService  csvService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping
    public ResponseEntity<List<SportsAuctionConfigResponse>> getConfigs(
            @RequestParam Long sportId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_AUCTION_CONFIG, VIEW_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long communityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
        return ResponseEntity.ok(auctionService.getConfigResponsesBySportAndCommunity(sportId, communityId));
    }

    /** GET all configs for the user's community across all sports */
    @GetMapping("/all")
    public ResponseEntity<List<SportsAuctionConfigResponse>> getCommunityConfigs(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_AUCTION_CONFIG, VIEW_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long communityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
        return ResponseEntity.ok(auctionService.getConfigResponsesByCommunity(communityId));
    }

    /** GET check if auction config exists for the logged-in user's community */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkConfigExists(
            @RequestParam Long sportId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_AUCTION_CONFIG, VIEW_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long communityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
        List<SportsAuctionConfig> configs = auctionService.getConfigsBySportAndCommunity(sportId, communityId);
        boolean exists = !configs.isEmpty();
        return ResponseEntity.ok(Map.of(
                "configExists", exists,
                "configCount", configs.size(),
                "communityId", communityId != null ? communityId : 0
        ));
    }

    /** GET single config by ID */
    @GetMapping("/{id}")
    public ResponseEntity<SportsAuctionConfigResponse> getConfig(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_AUCTION_CONFIG, VIEW_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.getConfigResponse(id));
    }

    /** GET auction stats by config ID */
    @GetMapping("/{id}/stats")
    public ResponseEntity<com.manacommunity.api.dto.SportsAuctionStatsResponse> getStats(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_AUCTION_CONFIG, VIEW_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.getAuctionStats(id));
    }

    /** POST create new auction config (admin only) */
    @PostMapping
    public ResponseEntity<SportsAuctionConfigResponse> createConfig(
            @Valid @RequestBody SportsAuctionConfigRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_AUCTION_CONFIG);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        SportsAuctionConfig created = auctionService.createConfig(req, loggedInUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(auctionService.getConfigResponse(created.getId()));
    }

    /** PUT update auction rules dynamically — cannot update when LIVE */
    @PutMapping("/{id}")
    public ResponseEntity<SportsAuctionConfigResponse> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody SportsAuctionConfigRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_AUCTION_CONFIG);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        auctionService.updateConfig(id, req);
        return ResponseEntity.ok(auctionService.getConfigResponse(id));
    }

    /** PUT change auction status: DRAFT→ACTIVE→LIVE→COMPLETED */
    @PutMapping("/{id}/status")
    public ResponseEntity<SportsAuctionConfigResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        auctionService.updateStatus(id, status);
        return ResponseEntity.ok(auctionService.getConfigResponse(id));
    }

    /** POST upload players via CSV */
    @PostMapping("/{id}/players/upload")
    public ResponseEntity<SportsAuctionCsvService.UploadResult> uploadPlayers(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_PLAYER_POOL);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(csvService.uploadPlayersFromFile(id, file));
    }

    /** POST create single player manually */
    @PostMapping("/{id}/players")
    public ResponseEntity<com.manacommunity.api.dto.SportsAuctionPlayerResponse> createPlayer(
            @PathVariable Long id,
            @Valid @RequestBody com.manacommunity.api.dto.SportsAuctionPlayerRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_PLAYER_POOL);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(SportsAuctionPlayerController.toResponse(auctionService.createPlayer(id, req)));
    }

    /** GET /api/auction/config/{id}/registration-count — get confirmed registration count */
    @GetMapping("/{id}/registration-count")
    public ResponseEntity<Long> getRegistrationCount(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_AUCTION_CONFIG, VIEW_LIVE_AUCTION);
        return ResponseEntity.ok(auctionService.getConfirmedRegistrationCount(id));
    }
}
