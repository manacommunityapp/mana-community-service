package com.manacommunity.api.controller;

import com.manacommunity.api.dto.SportsAuctionTeamRequest;
import com.manacommunity.api.dto.SportsAuctionTeamResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.SportsAuctionTeam;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.service.SportsAuctionTeamService;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.service.PermissionCheckService;
import static com.manacommunity.api.constants.PermissionConstants.*;
import static com.manacommunity.api.constants.permissions.SportsPermissions.*;
import static com.manacommunity.api.constants.permissions.SportsPermissions.CREATE_EDIT_PLAYER_POOL;
import static com.manacommunity.api.constants.permissions.SportsPermissions.CREATE_EDIT_TEAMS_DASHBOARD;
import static com.manacommunity.api.constants.permissions.SportsPermissions.VIEW_PLAYER_POOL;
import static com.manacommunity.api.constants.permissions.SportsPermissions.VIEW_TEAMS_DASHBOARD;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auction/teams")
@RequiredArgsConstructor
public class SportsAuctionTeamController {

    private final SportsAuctionTeamService auctionTeamService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping("/{configId}")
    public ResponseEntity<List<SportsAuctionTeamResponse>> getTeams(
            @PathVariable Long configId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_TEAMS_DASHBOARD, VIEW_PLAYER_POOL);
        return ResponseEntity.ok(auctionTeamService.getTeams(configId).stream().map(this::toResponse).toList());
    }

    @GetMapping("/nominated/{eventId}")
    public ResponseEntity<List<SportsAuctionTeamResponse>> getNominatedCaptains(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_TEAMS_DASHBOARD, VIEW_PLAYER_POOL);
        return ResponseEntity.ok(auctionTeamService.getNominatedCaptains(eventId).stream().map(this::toResponse).toList());
    }

    @GetMapping("/captain/mine")
    public ResponseEntity<List<SportsAuctionTeamResponse>> getMyCaptainRegistrations(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_PLAYER_POOL);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionTeamService.getCaptainRegistration(loggedInUser.getId()).stream().map(this::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<SportsAuctionTeamResponse> createTeam(
            @Valid @RequestBody SportsAuctionTeamRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_TEAMS_DASHBOARD);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        SportsAuctionTeam created = auctionTeamService.createTeam(req, loggedInUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{teamId}/confirm-captain")
    public ResponseEntity<SportsAuctionTeamResponse> confirmCaptain(
            @PathVariable Long teamId,
            @RequestParam boolean confirm,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_PLAYER_POOL);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        boolean isAdmin = permissionCheckService.hasAnyPermission(principal, CREATE_EDIT_TEAMS_DASHBOARD);
        return ResponseEntity.ok(toResponse(auctionTeamService.confirmCaptain(teamId, confirm, loggedInUser.getId(), isAdmin)));
    }

    @PostMapping("/nominate")
    public ResponseEntity<SportsAuctionTeamResponse> nominateCaptain(
            @RequestParam Long eventId,
            @RequestParam boolean nominate,
            @RequestParam(required = false) String teamName,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_PLAYER_POOL);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(toResponse(auctionTeamService.nominateCaptain(eventId, loggedInUser.getId(), nominate, teamName)));
    }

    @GetMapping("/captain/all")
    public ResponseEntity<List<SportsAuctionTeamResponse>> getAllCaptainNominations(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_PLAYER_POOL);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionTeamService.getCaptainRegistration(loggedInUser.getId()).stream().map(this::toResponse).toList());
    }

    private SportsAuctionTeamResponse toResponse(SportsAuctionTeam t) {
        return SportsAuctionTeamResponse.builder()
                .id(t.getId())
                .configId(t.getConfig() != null ? t.getConfig().getId() : null)
                .eventId(t.getEventId())
                .teamName(t.getTeamName())
                .ownerName(t.getOwnerName())
                .ownerUserId(t.getOwnerUser() != null ? t.getOwnerUser().getId() : null)
                .captainUserId(t.getCaptainUser() != null ? t.getCaptainUser().getId() : null)
                .colorHex(t.getColorHex())
                .totalBudget(t.getTotalBudget())
                .remainingBudget(t.getRemainingBudget())
                .spent(t.getSpent())
                .captainNomination(t.getCaptainNomination())
                .captainConfirmation(t.getCaptainConfirmation())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
