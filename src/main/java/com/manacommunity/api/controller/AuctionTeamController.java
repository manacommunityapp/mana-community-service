package com.manacommunity.api.controller;

import com.manacommunity.api.dto.AuctionTeamRequest;
import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.model.AuctionTeam;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.AuctionTeamService;
import com.manacommunity.api.service.LoggedInUserService;
import com.manacommunity.api.service.PermissionCheckService;
import static com.manacommunity.api.constants.PermissionConstants.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auction/teams")
@RequiredArgsConstructor
public class AuctionTeamController {

    private final AuctionTeamService auctionTeamService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping("/{configId}")
    public ResponseEntity<List<AuctionTeam>> getTeams(
            @PathVariable Long configId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_TEAMS_DASHBOARD, VIEW_PLAYER_POOL);
        return ResponseEntity.ok(auctionTeamService.getTeams(configId));
    }

    @GetMapping("/nominated/{eventId}")
    public ResponseEntity<List<AuctionTeam>> getNominatedCaptains(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_TEAMS_DASHBOARD, VIEW_PLAYER_POOL);
        return ResponseEntity.ok(auctionTeamService.getNominatedCaptains(eventId));
    }

    @GetMapping("/captain/mine")
    public ResponseEntity<List<AuctionTeam>> getMyCaptainRegistrations(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_PLAYER_POOL);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionTeamService.getCaptainRegistration(loggedInUser.getId()));
    }

    @PostMapping
    public ResponseEntity<AuctionTeam> createTeam(
            @RequestBody AuctionTeamRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_TEAMS_DASHBOARD);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(auctionTeamService.createTeam(req, loggedInUser.getId()));
    }

    @PutMapping("/{teamId}/confirm-captain")
    public ResponseEntity<AuctionTeam> confirmCaptain(
            @PathVariable Long teamId,
            @RequestParam boolean confirm,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_PLAYER_POOL);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        boolean isAdmin = permissionCheckService.hasAnyPermission(principal, CREATE_EDIT_TEAMS_DASHBOARD);
        return ResponseEntity.ok(auctionTeamService.confirmCaptain(teamId, confirm, loggedInUser.getId(), isAdmin));
    }

    @PostMapping("/nominate")
    public ResponseEntity<AuctionTeam> nominateCaptain(
            @RequestParam Long eventId,
            @RequestParam boolean nominate,
            @RequestParam(required = false) String teamName,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_PLAYER_POOL);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionTeamService.nominateCaptain(eventId, loggedInUser.getId(), nominate, teamName));
    }

    @GetMapping("/captain/all")
    public ResponseEntity<List<AuctionTeam>> getAllCaptainNominations(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_PLAYER_POOL);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionTeamService.getCaptainRegistration(loggedInUser.getId()));
    }
}
