package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.entity.PoojaType;
import com.manacommunity.api.events.service.PoojaTypeService;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events/pooja-types")
public class PoojaTypeController {

    private final PoojaTypeService poojaTypeService;

    public PoojaTypeController(PoojaTypeService poojaTypeService) {
        this.poojaTypeService = poojaTypeService;
    }

    @GetMapping
    public ResponseEntity<List<PoojaType>> getPoojaTypes(@AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        List<PoojaType> types = poojaTypeService.getAllPoojaTypes(communityId);
        return ResponseEntity.ok(types);
    }

    @PostMapping
    public ResponseEntity<PoojaType> createPoojaType(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        String name = body.get("name");
        String description = body.get("description");
        PoojaType created = poojaTypeService.createPoojaType(communityId, name, description);
        return ResponseEntity.ok(created);
    }
}
