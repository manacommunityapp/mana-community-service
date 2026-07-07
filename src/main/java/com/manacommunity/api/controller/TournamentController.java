package com.manacommunity.api.controller;

import static com.manacommunity.api.constants.PermissionConstants.*;
import com.manacommunity.api.dto.SponsorDto;
import com.manacommunity.api.dto.TournamentRequest;
import com.manacommunity.api.dto.TournamentResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.Tournament;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.service.SportsEventService;
import com.manacommunity.api.service.TournamentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;

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
    public ResponseEntity<List<TournamentResponse>> getAllTournaments(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tournamentService.getAllTournaments().stream().map(this::toResponse).toList());
    }

    @GetMapping("/community")
    public ResponseEntity<List<TournamentResponse>> getCommunityTournaments(
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
        return ResponseEntity.ok(tournamentService.getCommunityTournaments(targetCommunityId)
                .stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentResponse> getTournamentById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(tournamentService.getTournamentById(id)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTournament(@PathVariable Long id) {
        tournamentService.deleteTournament(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<TournamentResponse> createTournament(
            @Valid @RequestBody TournamentRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Tournament tournament = tournamentService.saveTournamentRecord(req, req.getAllowAdminChat());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(tournament));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<TournamentResponse> updateStatus(
            @PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(toResponse(tournamentService.updateStatus(id, status)));
    }

    private TournamentResponse toResponse(Tournament t) {
        List<SponsorDto> sponsorDtos = t.getSponsors() != null
                ? t.getSponsors().stream().map(s -> {
                    SponsorDto dto = new SponsorDto();
                    dto.setCategory(s.getCategory());
                    dto.setName(s.getName());
                    dto.setUrl(s.getUrl());
                    return dto;
                }).toList()
                : List.of();

        return TournamentResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .communityId(t.getCommunity() != null ? t.getCommunity().getId() : null)
                .communityName(t.getCommunity() != null ? t.getCommunity().getName() : null)
                .maxParticipants(t.getMaxParticipants())
                .description(t.getDescription())
                .eventDateStart(t.getEventDateStart())
                .eventDateEnd(t.getEventDateEnd())
                .registrationDateStart(t.getRegistrationDateStart())
                .registrationDateEnd(t.getRegistrationDateEnd())
                .startTime(t.getStartTime())
                .dueTime(t.getDueTime())
                .bannerImage(t.getBannerImage())
                .contactName(t.getContactName())
                .contactNumber(t.getContactNumber())
                .contactEmail(t.getContactEmail())
                .otherContacts(t.getOtherContacts())
                .allowAdminChat(t.getAllowAdminChat())
                .registrationStatus(t.getRegistrationStatus() != null ? t.getRegistrationStatus().name() : null)
                .sponsors(sponsorDtos)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
