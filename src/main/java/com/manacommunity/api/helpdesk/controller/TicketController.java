package com.manacommunity.api.helpdesk.controller;

import com.manacommunity.api.helpdesk.dto.HelpdeskAnalyticsResponse;
import com.manacommunity.api.helpdesk.dto.TicketFeedbackRequest;
import com.manacommunity.api.helpdesk.dto.TicketRequest;
import com.manacommunity.api.helpdesk.dto.TicketResponse;
import com.manacommunity.api.helpdesk.service.TicketService;
import com.manacommunity.api.user.model.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/helpdesk/tickets")
@RequiredArgsConstructor
@Tag(name = "Helpdesk & SLA", description = "Enterprise community helpdesk, SLA tracking, and ticket management APIs")
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    @Operation(summary = "Get all tickets in the community with optional status filter")
    public ResponseEntity<List<TicketResponse>> getCommunityTickets(
            @RequestParam Long communityId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ticketService.getCommunityTickets(communityId, status));
    }

    @GetMapping("/open")
    @Operation(summary = "Get active open/in-progress tickets")
    public ResponseEntity<List<TicketResponse>> getOpenTickets(@RequestParam Long communityId) {
        return ResponseEntity.ok(ticketService.getOpenTickets(communityId));
    }

    @GetMapping("/my")
    @Operation(summary = "Get tickets raised by current user")
    public ResponseEntity<List<TicketResponse>> getMyTickets(@AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(ticketService.getMyTickets(currentUser.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ticket details by ID")
    public ResponseEntity<TicketResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(ticketService.getById(id, currentUser));
    }

    @PostMapping
    @Operation(summary = "Raise a new helpdesk ticket with SLA calculation")
    public ResponseEntity<TicketResponse> create(
            @Valid @RequestBody TicketRequest req,
            @AuthenticationPrincipal AppUser currentUser) {
        return new ResponseEntity<>(ticketService.create(req, currentUser, currentUser.getCommunity()), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update ticket status (Admin/Staff)")
    public ResponseEntity<TicketResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String remarks,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(ticketService.updateStatus(id, status, remarks, currentUser));
    }

    @PostMapping("/{id}/feedback")
    @Operation(summary = "Submit resident sign-off confirmation and CSAT satisfaction rating")
    public ResponseEntity<TicketResponse> submitFeedback(
            @PathVariable Long id,
            @Valid @RequestBody TicketFeedbackRequest feedback,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(ticketService.submitResidentFeedback(id, feedback, currentUser));
    }

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Assign ticket to technician or manager")
    public ResponseEntity<TicketResponse> assign(
            @PathVariable Long id,
            @RequestParam Long assigneeId,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(ticketService.assign(id, assigneeId, currentUser));
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add comment to a ticket thread")
    public ResponseEntity<TicketResponse> addComment(
            @PathVariable Long id,
            @RequestParam String message,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(ticketService.addComment(id, message, currentUser));
    }

    @PostMapping("/{id}/escalate-check")
    @Operation(summary = "Trigger SLA check and auto-escalate if breached")
    public ResponseEntity<TicketResponse> checkAndEscalate(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.checkAndEscalate(id));
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get helpdesk SLA and performance analytics for a community")
    public ResponseEntity<HelpdeskAnalyticsResponse> getAnalytics(@RequestParam Long communityId) {
        return ResponseEntity.ok(ticketService.getAnalytics(communityId));
    }
}
