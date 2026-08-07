package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.*;
import com.manacommunity.api.events.service.EventSubResourceService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for event sub-resources: Tasks, Volunteers, Sponsors.
 * <p>
 * Uses explicit paths (/tasks, /volunteers, /sponsors) that are resolved BEFORE
 * the catch-all /{id} in EventController because Spring evaluates more-specific
 * (literal) segments first. Having them in a separate controller guarantees ordering.
 */
// Disabled: Dedicated controllers (EventTaskController, EventVolunteerController, EventSponsorController, EventExpenseController) handle these routes.
// @RestController
// @RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventSubResourceController {

    private final EventSubResourceService service;

    // ── Tasks ────────────────────────────────────────────────────────────────────

    @GetMapping("/tasks")
    public ResponseEntity<List<EventTaskResponse>> getTasks(
            @RequestParam(required = false) Long eventId) {
        return ResponseEntity.ok(service.getTasks(eventId));
    }

    @PostMapping("/tasks")
    public ResponseEntity<EventTaskResponse> createTask(@RequestBody EventTaskRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createTask(req));
    }

    @PutMapping("/tasks/{id}")
    public ResponseEntity<EventTaskResponse> updateTask(
            @PathVariable Long id,
            @RequestBody EventTaskRequest req) {
        return ResponseEntity.ok(service.updateTask(id, req));
    }

    @PutMapping("/tasks/{id}/toggle")
    public ResponseEntity<EventTaskResponse> toggleDone(@PathVariable Long id) {
        return ResponseEntity.ok(service.toggleDone(id));
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        service.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    // ── Volunteers ───────────────────────────────────────────────────────────────

    @GetMapping("/volunteers")
    public ResponseEntity<List<EventVolunteerResponse>> getVolunteers(
            @RequestParam(required = false) Long eventId) {
        return ResponseEntity.ok(service.getVolunteers(eventId));
    }

    @PostMapping("/volunteers")
    public ResponseEntity<EventVolunteerResponse> createVolunteer(
            @RequestBody EventVolunteerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createVolunteer(req));
    }

    @PutMapping("/volunteers/{id}")
    public ResponseEntity<EventVolunteerResponse> updateVolunteer(
            @PathVariable Long id,
            @RequestBody EventVolunteerRequest req) {
        return ResponseEntity.ok(service.updateVolunteer(id, req));
    }

    @PutMapping("/volunteers/{id}/check-in")
    public ResponseEntity<EventVolunteerResponse> checkIn(@PathVariable Long id) {
        return ResponseEntity.ok(service.checkIn(id));
    }

    @PutMapping("/volunteers/{id}/check-out")
    public ResponseEntity<EventVolunteerResponse> checkOut(@PathVariable Long id) {
        return ResponseEntity.ok(service.checkOut(id));
    }

    @DeleteMapping("/volunteers/{id}")
    public ResponseEntity<Void> deleteVolunteer(@PathVariable Long id) {
        service.deleteVolunteer(id);
        return ResponseEntity.noContent().build();
    }

    // ── Sponsors ─────────────────────────────────────────────────────────────────

    @GetMapping("/sponsors")
    public ResponseEntity<List<EventSponsorResponse>> getSponsors(
            @RequestParam(required = false) Long eventId) {
        return ResponseEntity.ok(service.getSponsors(eventId));
    }

    @PostMapping("/sponsors")
    public ResponseEntity<EventSponsorResponse> createSponsor(
            @RequestBody EventSponsorRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSponsor(req));
    }

    @PutMapping("/sponsors/{id}")
    public ResponseEntity<EventSponsorResponse> updateSponsor(
            @PathVariable Long id,
            @RequestBody EventSponsorRequest req) {
        return ResponseEntity.ok(service.updateSponsor(id, req));
    }

    @DeleteMapping("/sponsors/{id}")
    public ResponseEntity<Void> deleteSponsor(@PathVariable Long id) {
        service.deleteSponsor(id);
        return ResponseEntity.noContent().build();
    }

    // ── Expenses ─────────────────────────────────────────────────────────────────

    @GetMapping("/expenses")
    public ResponseEntity<List<EventExpenseResponse>> getExpenses(
            @RequestParam(required = false) Long eventId) {
        return ResponseEntity.ok(service.getExpenses(eventId));
    }

    @PostMapping("/expenses")
    public ResponseEntity<EventExpenseResponse> createExpense(
            @RequestBody EventExpenseRequest req,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createExpense(req, currentUser));
    }

    @PutMapping("/expenses/{id}")
    public ResponseEntity<EventExpenseResponse> updateExpense(
            @PathVariable Long id,
            @RequestBody EventExpenseRequest req) {
        return ResponseEntity.ok(service.updateExpense(id, req));
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        service.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }
}
