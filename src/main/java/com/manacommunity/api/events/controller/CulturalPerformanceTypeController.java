package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.entity.EventCulturalPerformanceType;
import com.manacommunity.api.events.service.CulturalPerformanceTypeService;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events/cultural-performance-types")
public class CulturalPerformanceTypeController {

    private final CulturalPerformanceTypeService service;

    public CulturalPerformanceTypeController(CulturalPerformanceTypeService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<EventCulturalPerformanceType>> getPerformanceTypes(@AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        return ResponseEntity.ok(service.getAllPerformanceTypes(communityId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<EventCulturalPerformanceType> createPerformanceType(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        EventCulturalPerformanceType created = service.createPerformanceType(communityId, body.get("name"), body.get("description"));
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<EventCulturalPerformanceType> updatePerformanceType(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        EventCulturalPerformanceType updated = service.updatePerformanceType(id, communityId, body.get("name"), body.get("description"));
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<Void> deletePerformanceType(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        service.deletePerformanceType(id, communityId);
        return ResponseEntity.noContent().build();
    }
}
