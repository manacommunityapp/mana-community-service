package com.manacommunity.api.safety.controller;

import com.manacommunity.api.safety.dto.AnprEventResponse;
import com.manacommunity.api.safety.dto.AnprSummaryResponse;
import com.manacommunity.api.safety.model.AnprEvent;
import com.manacommunity.api.safety.service.AnprEventService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/safety/anpr")
@RequiredArgsConstructor
public class AnprController {

    private final AnprEventService anprEventService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/events")
    public ResponseEntity<List<AnprEventResponse>> getEvents(
            @RequestParam(required = false) String gate,
            @RequestParam(required = false) AnprEvent.Direction direction,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(anprEventService.getEvents(communityId, gate, direction));
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<AnprEventResponse> getEventById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(anprEventService.getEventById(id));
    }

    @GetMapping("/summary")
    public ResponseEntity<AnprSummaryResponse> getSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(new AnprSummaryResponse());
        return ResponseEntity.ok(anprEventService.getSummary(communityId));
    }
}
