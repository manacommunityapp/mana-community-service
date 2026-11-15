package com.manacommunity.api.sports.controller;

import com.manacommunity.api.sports.dto.TournamentBudgetRequest;
import com.manacommunity.api.sports.dto.TournamentBudgetResponse;
import com.manacommunity.api.sports.service.TournamentBudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sports/budgets")
@RequiredArgsConstructor
public class TournamentBudgetController {

    private final TournamentBudgetService budgetService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN')")
    public ResponseEntity<List<TournamentBudgetResponse>> listByCommunity(
            @RequestParam Long communityId) {
        return ResponseEntity.ok(budgetService.listByCommunity(communityId));
    }

    @GetMapping("/tournament/{tournamentId}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN')")
    public ResponseEntity<TournamentBudgetResponse> getByTournament(@PathVariable Long tournamentId) {
        return ResponseEntity.ok(budgetService.getByTournament(tournamentId));
    }

    @PutMapping("/tournament/{tournamentId}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN')")
    public ResponseEntity<TournamentBudgetResponse> createOrUpdate(
            @PathVariable Long tournamentId,
            @Valid @RequestBody TournamentBudgetRequest request) {
        return ResponseEntity.ok(budgetService.createOrUpdate(tournamentId, request));
    }

    @DeleteMapping("/tournament/{tournamentId}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long tournamentId) {
        budgetService.delete(tournamentId);
        return ResponseEntity.noContent().build();
    }
}
