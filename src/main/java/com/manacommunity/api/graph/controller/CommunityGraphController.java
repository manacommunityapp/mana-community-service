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

        List<Map<String, Object>> neighbors = appUserRepo
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

        return ResponseEntity.ok(Map.of(
                "neighbors", neighbors,
                "clubs", List.of()
        ));
    }
}
