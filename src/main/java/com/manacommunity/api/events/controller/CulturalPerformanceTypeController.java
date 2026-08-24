package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.entity.EventCulturalPerformanceType;
import com.manacommunity.api.events.service.CulturalPerformanceTypeService;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
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
        List<EventCulturalPerformanceType> types = service.getAllPerformanceTypes(communityId);
        return ResponseEntity.ok(types);
    }

    @PostMapping
    public ResponseEntity<EventCulturalPerformanceType> createPerformanceType(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        String name = body.get("name");
        String description = body.get("description");
        EventCulturalPerformanceType created = service.createPerformanceType(communityId, name, description);
        return ResponseEntity.ok(created);
    }
}
