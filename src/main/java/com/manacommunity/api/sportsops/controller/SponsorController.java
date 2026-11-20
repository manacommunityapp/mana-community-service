package com.manacommunity.api.sportsops.controller;

import com.manacommunity.api.sportsops.entity.TournamentSponsor;
import com.manacommunity.api.sportsops.service.SponsorService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sports-ops/sponsors")
@RequiredArgsConstructor
public class SponsorController {
    private final SponsorService service;

    @GetMapping
    public List<TournamentSponsor> getForTournament(@RequestParam Long tournamentId) {
        return service.getForTournament(tournamentId);
    }

    @PostMapping
    public ResponseEntity<TournamentSponsor> add(
            @RequestBody TournamentSponsor sponsor,
            @AuthenticationPrincipal UserPrincipal principal) {
        sponsor.setCommunityId(principal.getCommunityId());
        return ResponseEntity.status(201).body(service.add(sponsor));
    }

    @PutMapping("/{id}/status")
    public TournamentSponsor updateStatus(@PathVariable Long id,
                                           @RequestBody Map<String, String> body) {
        return service.updateStatus(id, body.get("status"));
    }
}
