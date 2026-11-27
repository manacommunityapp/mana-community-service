package com.manacommunity.api.helpdesk.controller;

import com.manacommunity.api.helpdesk.dto.*;
import com.manacommunity.api.helpdesk.service.TicketService;
import com.manacommunity.api.user.model.AppUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Resident-facing helpdesk overview and ticket endpoints at /api/helpdesk.
 */
@RestController
@RequestMapping("/api/helpdesk")
@RequiredArgsConstructor
public class HelpdeskController {

    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getCommunityTickets(
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal AppUser currentUser) {
        Long cId = communityId != null ? communityId : (currentUser != null && currentUser.getCommunity() != null ? currentUser.getCommunity().getId() : 1L);
        return ResponseEntity.ok(ticketService.getCommunityTickets(cId, status));
    }

    @GetMapping("/open")
    public ResponseEntity<List<TicketResponse>> getOpenTickets(
            @RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal AppUser currentUser) {
        Long cId = communityId != null ? communityId : (currentUser != null && currentUser.getCommunity() != null ? currentUser.getCommunity().getId() : 1L);
        return ResponseEntity.ok(ticketService.getOpenTickets(cId));
    }

    @GetMapping({"/my", "/mine"})
    public ResponseEntity<List<TicketResponse>> getMyTickets(@AuthenticationPrincipal AppUser currentUser) {
        if (currentUser == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(ticketService.getMyTickets(currentUser.getId()));
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<TicketResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(ticketService.getById(id, currentUser));
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(
            @Valid @RequestBody TicketRequest req,
            @AuthenticationPrincipal AppUser currentUser) {
        return new ResponseEntity<>(ticketService.create(req, currentUser, currentUser != null ? currentUser.getCommunity() : null), HttpStatus.CREATED);
    }

    @PostMapping({"/ai-classify", "/tickets/ai-classify"})
    public ResponseEntity<HelpdeskAiDtos.AiClassificationResult> classifyAi(
            @Valid @RequestBody HelpdeskAiDtos.AiClassificationRequest req) {
        return ResponseEntity.ok(ticketService.classifyAi(req));
    }

    @PostMapping({"/{id:[0-9]+}/resolve", "/tickets/{id:[0-9]+}/resolve"})
    public ResponseEntity<TicketResponse> resolveTicket(
            @PathVariable Long id,
            @Valid @RequestBody HelpdeskAiDtos.TicketResolutionRequest req,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(ticketService.resolveTicket(id, req, currentUser));
    }

    @PostMapping({"/{id:[0-9]+}/reopen", "/tickets/{id:[0-9]+}/reopen"})
    public ResponseEntity<TicketResponse> reopenTicket(
            @PathVariable Long id,
            @Valid @RequestBody HelpdeskAiDtos.TicketReopenRequest req,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(ticketService.reopenTicket(id, req, currentUser));
    }

    @PostMapping({"/{id:[0-9]+}/feedback", "/tickets/{id:[0-9]+}/feedback"})
    public ResponseEntity<TicketResponse> submitFeedback(
            @PathVariable Long id,
            @Valid @RequestBody TicketFeedbackRequest feedback,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(ticketService.submitResidentFeedback(id, feedback, currentUser));
    }

    @RequestMapping(value = {"/{id:[0-9]+}/status", "/tickets/{id:[0-9]+}/status"}, method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<TicketResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String remarks,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal AppUser currentUser) {
        String resolvedStatus = status != null ? status : (body != null ? body.get("status") : "OPEN");
        String resolvedRemarks = remarks != null ? remarks : (body != null ? body.get("remarks") : null);
        return ResponseEntity.ok(ticketService.updateStatus(id, resolvedStatus, resolvedRemarks, currentUser));
    }

    @RequestMapping(value = {"/{id:[0-9]+}/assign", "/tickets/{id:[0-9]+}/assign"}, method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<TicketResponse> assign(
            @PathVariable Long id,
            @RequestParam(required = false) Long assigneeId,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal AppUser currentUser) {
        Long resolvedAssignee = assigneeId != null ? assigneeId : (body != null && body.get("assigneeId") != null ? Long.valueOf(body.get("assigneeId").toString()) : 1L);
        return ResponseEntity.ok(ticketService.assign(id, resolvedAssignee, currentUser));
    }

    @PostMapping({"/{id:[0-9]+}/comments", "/tickets/{id:[0-9]+}/comments"})
    public ResponseEntity<TicketResponse> addComment(
            @PathVariable Long id,
            @RequestParam(required = false) String message,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal AppUser currentUser) {
        String resolvedMsg = message != null ? message : (body != null ? body.get("message") : "");
        return ResponseEntity.ok(ticketService.addComment(id, resolvedMsg, currentUser));
    }

    @GetMapping("/sla-policies")
    public ResponseEntity<List<Map<String, Object>>> getSlaPolicies(
            @RequestParam(required = false) Long communityId) {
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/work-orders")
    public ResponseEntity<List<Map<String, Object>>> getWorkOrders(
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/dashboard/resident")
    public ResponseEntity<Map<String, Object>> residentDashboard(
            @RequestParam Long userId,
            @RequestParam(required = false) Long communityId) {
        return ResponseEntity.ok(Map.of(
                "openTickets", 0,
                "resolvedThisMonth", 0,
                "avgResolutionDays", 0.0,
                "pendingWorkOrders", 0,
                "recentTickets", List.of()
        ));
    }
}
