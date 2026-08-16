package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.entity.CompetitionCategory;
import com.manacommunity.api.events.service.CompetitionCategoryService;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events/competition-categories")
public class CompetitionCategoryController {

    private final CompetitionCategoryService service;

    public CompetitionCategoryController(CompetitionCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<CompetitionCategory>> getCompetitionCategories(@AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        List<CompetitionCategory> categories = service.getAllCompetitionCategories(communityId);
        return ResponseEntity.ok(categories);
    }

    @PostMapping
    public ResponseEntity<CompetitionCategory> createCompetitionCategory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        String name = body.get("name");
        String description = body.get("description");
        CompetitionCategory created = service.createCompetitionCategory(communityId, name, description);
        return ResponseEntity.ok(created);
    }
}
