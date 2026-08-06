package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.EventProgramRequest;
import com.manacommunity.api.events.dto.EventProgramResponse;
import com.manacommunity.api.events.service.EventProgramService;
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
@RequestMapping("/api/events/programs")
@RequiredArgsConstructor
public class EventProgramController {

    private final EventProgramService programService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<List<EventProgramResponse>> getByEvent(
            @RequestParam Long eventId,
            @RequestParam(required = false) String dayLabel) {
        return ResponseEntity.ok(programService.getByEvent(eventId, dayLabel));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<EventProgramResponse> create(
            @Valid @RequestBody EventProgramRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(programService.create(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<EventProgramResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EventProgramRequest req) {
        return ResponseEntity.ok(programService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        programService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
