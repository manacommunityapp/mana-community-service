package com.manacommunity.api.helpdesk.controller;

import com.manacommunity.api.helpdesk.dto.TicketRequest;
import com.manacommunity.api.helpdesk.dto.TicketResponse;
import com.manacommunity.api.helpdesk.service.TicketService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/helpdesk")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('Manage Tickets')")
    public ResponseEntity<List<TicketResponse>> getCommunityTickets(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(ticketService.getCommunityTickets(communityId, status));
    }

    @GetMapping("/open")
    @PreAuthorize("hasAuthority('Manage Tickets')")
    public ResponseEntity<List<TicketResponse>> getOpenTickets(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(ticketService.getOpenTickets(communityId));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('View Tickets')")
    public ResponseEntity<List<TicketResponse>> getMyTickets(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(ticketService.getMyTickets(user.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Tickets')")
    public ResponseEntity<TicketResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(ticketService.getById(id, user));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Ticket')")
    public ResponseEntity<TicketResponse> create(
            @Valid @RequestBody TicketRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.create(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('Manage Tickets')")
    public ResponseEntity<TicketResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(ticketService.updateStatus(id, body.get("status"), body.get("remarks"), user));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('Manage Tickets')")
    public ResponseEntity<TicketResponse> assign(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(ticketService.assign(id, body.get("assigneeId"), user));
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAuthority('View Tickets')")
    public ResponseEntity<TicketResponse> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(ticketService.addComment(id, body.get("message"), user));
    }
}
