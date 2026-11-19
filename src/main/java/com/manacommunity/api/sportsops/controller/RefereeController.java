package com.manacommunity.api.sportsops.controller;

import com.manacommunity.api.sportsops.entity.RefereeAssignment;
import com.manacommunity.api.sportsops.service.RefereeService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sports-ops/referees")
@RequiredArgsConstructor
public class RefereeController {
    private final RefereeService service;

    @GetMapping
    public List<RefereeAssignment> list(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listByCommunity(principal.getCommunityId());
    }

    @GetMapping("/my")
    public List<RefereeAssignment> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listByReferee(principal.getCommunityId(), principal.getUserId());
    }

    @PostMapping
    public RefereeAssignment assign(@AuthenticationPrincipal UserPrincipal principal,
                                     @RequestBody RefereeAssignment assignment) {
        assignment.setCommunityId(principal.getCommunityId());
        return service.assign(assignment);
    }

    @PutMapping("/{id}/confirm")
    public RefereeAssignment confirm(@AuthenticationPrincipal UserPrincipal principal,
                                      @PathVariable Long id) {
        return service.confirm(principal.getCommunityId(), id);
    }
}
