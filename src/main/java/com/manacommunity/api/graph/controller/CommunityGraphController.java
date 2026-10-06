package com.manacommunity.api.graph.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping({"/api/graph", "/api/community-graph"})
@RequiredArgsConstructor
public class CommunityGraphController {

    private final AppUserRepository appUserRepo;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/discover")
    public ResponseEntity<Map<String, Object>> discover(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser me = loggedInUserService.resolve(principal);
        if (me.getCommunity() == null) {
            return ResponseEntity.ok(Map.of("neighbors", List.of(), "clubs", List.of()));
        }
        Long communityId = me.getCommunity().getId();

        List<Map<String, Object>> neighbors = getNeighborProfiles(me, communityId);

        return ResponseEntity.ok(Map.of(
                "neighbors", neighbors,
                "clubs", List.of()
        ));
    }

    @GetMapping("/discover/search")
    public ResponseEntity<?> searchMembers(
            @RequestParam(value = "q", required = false) String query,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser me = loggedInUserService.resolve(principal);
        if (me.getCommunity() == null) {
            return ResponseEntity.ok(Map.of("results", List.of()));
        }
        Long communityId = me.getCommunity().getId();
        String q = query != null ? query.toLowerCase() : "";

        List<Map<String, Object>> results = appUserRepo
                .findByCommunityIdAndIsActiveTrue(communityId)
                .stream()
                .filter(u -> !u.getId().equals(me.getId()))
                .filter(u -> q.isEmpty()
                        || (u.getFullName() != null && u.getFullName().toLowerCase().contains(q))
                        || (u.getFlatNo() != null && u.getFlatNo().toLowerCase().contains(q)))
                .limit(30)
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("name", u.getFullName());
                    m.put("flatNumber", u.getFlatNo());
                    m.put("profileImageUrl", u.getProfilePicUrl());
                    m.put("type", "PERSON");
                    return m;
                })
                .toList();

        return ResponseEntity.ok(Map.of("results", results));
    }

    @GetMapping("/discover/skills")
    public ResponseEntity<?> getSkillDirectory(
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(List.of());
    }

    @PostMapping({"/connect/{targetUserId}", "/relationships"})
    public ResponseEntity<?> connectWithNeighbor(
            @PathVariable(required = false) Long targetUserId,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        Long target = targetUserId;
        if (target == null && body != null) {
            Object tid = body.get("targetId");
            if (tid instanceof Number) target = ((Number) tid).longValue();
            else if (tid != null) target = Long.parseLong(tid.toString());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "CONNECTED");
        result.put("targetUserId", target);
        return ResponseEntity.ok(result);
    }

    private List<Map<String, Object>> getNeighborProfiles(AppUser me, Long communityId) {
        return appUserRepo
                .findByCommunityIdAndIsActiveTrue(communityId)
                .stream()
                .filter(u -> !u.getId().equals(me.getId()))
                .limit(50)
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("name", u.getFullName());
                    m.put("flatNumber", u.getFlatNo());
                    m.put("profileImageUrl", u.getProfilePicUrl());
                    m.put("interests", List.of());
                    m.put("mutualConnections", 0);
                    return m;
                })
                .toList();
    }
}
