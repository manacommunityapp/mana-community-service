package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.entity.PoojaSeva;
import com.manacommunity.api.events.service.PoojaSevaService;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/pooja-sevas")
public class PoojaSevaController {

    private final PoojaSevaService service;

    public PoojaSevaController(PoojaSevaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<PoojaSeva>> getAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long mainEventId) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        List<PoojaSeva> list = service.getAllPoojaSevas(communityId, mainEventId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PoojaSeva> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        PoojaSeva item = service.getPoojaSevaById(id, communityId);
        return ResponseEntity.ok(item);
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<PoojaSeva> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody PoojaSeva body) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        PoojaSeva created = service.createPoojaSeva(communityId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<PoojaSeva> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody PoojaSeva body) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        PoojaSeva updated = service.updatePoojaSeva(id, communityId, body);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        service.deletePoojaSeva(id, communityId);
        return ResponseEntity.noContent().build();
    }
}
