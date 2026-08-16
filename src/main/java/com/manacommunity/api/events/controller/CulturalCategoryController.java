package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.entity.CulturalCategory;
import com.manacommunity.api.events.service.CulturalCategoryService;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<CulturalCategory>> getCulturalCategories(@AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        List<CulturalCategory> categories = service.getAllCulturalCategories(communityId);
        return ResponseEntity.ok(categories);
    }

    @PostMapping
    public ResponseEntity<CulturalCategory> createCulturalCategory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        String name = body.get("name");
        String description = body.get("description");
        CulturalCategory created = service.createCulturalCategory(communityId, name, description);
        return ResponseEntity.ok(created);
    }
}
