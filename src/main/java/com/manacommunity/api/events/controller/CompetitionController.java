package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.entity.EventCompetition;
import com.manacommunity.api.events.service.CompetitionService;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/competitions")
public class CompetitionController {

    private final CompetitionService service;

    public CompetitionController(CompetitionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<EventCompetition>> getAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long mainEventId) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        List<EventCompetition> list = service.getAllCompetitions(communityId, mainEventId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventCompetition> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        EventCompetition item = service.getCompetitionById(id, communityId);
        return ResponseEntity.ok(item);
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<EventCompetition> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody EventCompetition body) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        EventCompetition created = service.createCompetition(communityId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<EventCompetition> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody EventCompetition body) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        EventCompetition updated = service.updateCompetition(id, communityId, body);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        service.deleteCompetition(id, communityId);
        return ResponseEntity.noContent().build();
    }
}
