package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.EventMediaDayRequest;
import com.manacommunity.api.events.dto.EventMediaDayResponse;
import com.manacommunity.api.events.service.EventMediaDayService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/media-days")
@RequiredArgsConstructor
public class EventMediaDayController {

    private final EventMediaDayService dayService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<List<EventMediaDayResponse>> getAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dayService.getAll(user.getCommunity().getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<EventMediaDayResponse> create(
            @Valid @RequestBody EventMediaDayRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dayService.create(req, user.getCommunity()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        dayService.delete(id, user.getCommunity().getId());
        return ResponseEntity.noContent().build();
    }
}
