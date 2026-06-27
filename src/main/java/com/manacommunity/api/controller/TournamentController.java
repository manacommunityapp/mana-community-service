package com.manacommunity.api.controller;

import static com.manacommunity.api.constants.PermissionConstants.*;
import com.manacommunity.api.dto.SportsEventRequest;
import com.manacommunity.api.dto.TournamentRequest;
import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.model.SportsEvent;
import com.manacommunity.api.model.Tournament;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.LoggedInUserService;
import com.manacommunity.api.service.SportsEventService;
import com.manacommunity.api.service.TournamentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;
    private final LoggedInUserService loggedInUserService;
    private final SportsEventService eventService;

    @GetMapping("/all")
    public ResponseEntity<List<Tournament>> getAllTournaments(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tournamentService.getAllTournaments());
    }

    @GetMapping("/community")
    public ResponseEntity<List<Tournament>> getCommunityTournaments(
            @RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long targetCommunityId = communityId;
        if (!ROLE_SUPER_ADMIN.equals(loggedInUser.getRole())) {
            targetCommunityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
        }
        
        if (targetCommunityId == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(tournamentService.getCommunityTournaments(targetCommunityId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tournament> getTournamentById(@PathVariable Long id) {
        return ResponseEntity.ok(tournamentService.getTournamentById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTournament(@PathVariable Long id) {
        tournamentService.deleteTournament(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<Tournament> createTournament(
            @Valid @RequestBody TournamentRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);

        // Save corresponding Tournament record to tournament table directly via TournamentRequest DTO
        Tournament tournament = tournamentService.saveTournamentRecord(req, req.getAllowAdminChat());

        return ResponseEntity.status(HttpStatus.CREATED).body(tournament);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Tournament> updateStatus(
            @PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(tournamentService.updateStatus(id, status));
    }

}
