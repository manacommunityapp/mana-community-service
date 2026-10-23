package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.EventExpenseRequest;
import com.manacommunity.api.events.dto.EventExpenseResponse;
import com.manacommunity.api.events.service.EventExpenseService;
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
@RequestMapping("/api/events/expenses")
@RequiredArgsConstructor
public class EventExpenseController {

    private final EventExpenseService expenseService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','USER','RESIDENT') or hasAuthority('View Events')")
    public ResponseEntity<List<EventExpenseResponse>> getAll(
            @RequestParam(required = false) Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (eventId != null) {
            return ResponseEntity.ok(expenseService.getByEvent(eventId));
        }
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(expenseService.getByCommunity(communityId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN') or hasAuthority('Create Event')")
    public ResponseEntity<EventExpenseResponse> create(
            @Valid @RequestBody EventExpenseRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expenseService.create(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN') or hasAuthority('Create Event')")
    public ResponseEntity<EventExpenseResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EventExpenseRequest req) {
        return ResponseEntity.ok(expenseService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN') or hasAuthority('Create Event')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        expenseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
