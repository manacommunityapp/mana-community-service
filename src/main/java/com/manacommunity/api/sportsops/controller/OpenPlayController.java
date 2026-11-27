package com.manacommunity.api.sportsops.controller;

import com.manacommunity.api.sportsops.entity.OpenPlayParticipant;
import com.manacommunity.api.sportsops.entity.OpenPlaySession;
import com.manacommunity.api.sportsops.service.OpenPlayService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sports-ops/open-play")
@RequiredArgsConstructor
public class OpenPlayController {
    private final OpenPlayService service;

    @GetMapping
    public List<OpenPlaySession> listOpen(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listOpen(principal.getCommunityId());
    }

    @GetMapping("/all")
    public List<OpenPlaySession> listAll(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listAll(principal.getCommunityId());
    }

    @PostMapping
    public OpenPlaySession create(@AuthenticationPrincipal UserPrincipal principal,
                                   @RequestBody OpenPlaySession session) {
        session.setCommunityId(principal.getCommunityId());
        session.setOrganizerId(principal.getUserId());
        return service.create(session);
    }

    @PostMapping("/{sessionId}/join")
    public OpenPlayParticipant join(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long sessionId) {
        return service.join(principal.getCommunityId(), sessionId, principal.getUserId());
    }

    @PutMapping("/{sessionId}/close")
    public OpenPlaySession close(@AuthenticationPrincipal UserPrincipal principal,
                                  @PathVariable Long sessionId) {
        return service.close(principal.getCommunityId(), sessionId);
    }

    @GetMapping("/{sessionId}/participants")
    public List<OpenPlayParticipant> participants(@PathVariable Long sessionId) {
        return service.participants(sessionId);
    }
}
