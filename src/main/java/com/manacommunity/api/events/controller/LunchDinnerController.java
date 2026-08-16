package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.entity.LunchDinner;
import com.manacommunity.api.events.service.LunchDinnerService;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/lunch-dinners")
public class LunchDinnerController {

    private final LunchDinnerService service;

    public LunchDinnerController(LunchDinnerService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<LunchDinner>> getAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long mainEventId) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        List<LunchDinner> list = service.getAllLunchDinners(communityId, mainEventId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LunchDinner> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        LunchDinner item = service.getLunchDinnerById(id, communityId);
        return ResponseEntity.ok(item);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<LunchDinner> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody LunchDinner body) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        LunchDinner created = service.createLunchDinner(communityId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<LunchDinner> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody LunchDinner body) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        LunchDinner updated = service.updateLunchDinner(id, communityId, body);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        service.deleteLunchDinner(id, communityId);
        return ResponseEntity.noContent().build();
    }
}
