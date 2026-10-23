package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.entity.EventCulturalCategory;
import com.manacommunity.api.events.service.CulturalCategoryService;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events/cultural-categories")
public class CulturalCategoryController {

    private final CulturalCategoryService service;

    public CulturalCategoryController(CulturalCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<EventCulturalCategory>> getCulturalCategories(@AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        return ResponseEntity.ok(service.getAllCulturalCategories(communityId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<EventCulturalCategory> createCulturalCategory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        EventCulturalCategory created = service.createCulturalCategory(communityId, body.get("name"), body.get("description"));
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<EventCulturalCategory> updateCulturalCategory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        EventCulturalCategory updated = service.updateCulturalCategory(id, communityId, body.get("name"), body.get("description"));
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<Void> deleteCulturalCategory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        service.deleteCulturalCategory(id, communityId);
        return ResponseEntity.noContent().build();
    }
}
