package com.manacommunity.api.helpdesk.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Resident-facing helpdesk overview endpoints at /api/helpdesk.
 * Ticket CRUD lives at /api/v1/helpdesk/tickets (TicketController).
 */
@RestController
@RequestMapping("/api/helpdesk")
@RequiredArgsConstructor
public class HelpdeskController {

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
