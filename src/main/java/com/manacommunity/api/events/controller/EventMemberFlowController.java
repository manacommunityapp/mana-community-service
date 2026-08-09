package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.EventMemberFlowResponse;
import com.manacommunity.api.events.service.EventMemberFlowService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events/member-flow")
@RequiredArgsConstructor
public class EventMemberFlowController {

    private final EventMemberFlowService memberFlowService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('View Events', 'Register Event')")
    public ResponseEntity<EventMemberFlowResponse> getMemberFlow(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(memberFlowService.getSummary(user));
    }
}
