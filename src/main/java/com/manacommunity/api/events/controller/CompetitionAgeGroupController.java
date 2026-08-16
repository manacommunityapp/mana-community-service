package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.entity.CompetitionAgeGroup;
import com.manacommunity.api.events.service.CompetitionAgeGroupService;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events/competition-age-groups")
public class CompetitionAgeGroupController {

    private final CompetitionAgeGroupService service;

    public CompetitionAgeGroupController(CompetitionAgeGroupService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<CompetitionAgeGroup>> getCompetitionAgeGroups(@AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        List<CompetitionAgeGroup> groups = service.getAllCompetitionAgeGroups(communityId);
        return ResponseEntity.ok(groups);
    }

    @PostMapping
    public ResponseEntity<CompetitionAgeGroup> createCompetitionAgeGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        String name = body.get("name");
        String description = body.get("description");
        CompetitionAgeGroup created = service.createCompetitionAgeGroup(communityId, name, description);
        return ResponseEntity.ok(created);
    }
}
