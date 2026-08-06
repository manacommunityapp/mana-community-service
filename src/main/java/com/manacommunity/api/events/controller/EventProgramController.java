package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.*;
import com.manacommunity.api.events.service.EventProgramService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventProgramController {

    private final EventProgramService service;

    // ── Programs CRUD ───────────────────────────────────────────────────────────

    @GetMapping("/programs")
    public ResponseEntity<List<EventProgramResponse>> getPrograms(
            @RequestParam Long eventId,
            @RequestParam(required = false) String dayLabel) {
        return ResponseEntity.ok(service.getPrograms(eventId, dayLabel));
    }

    @PostMapping("/programs")
    public ResponseEntity<EventProgramResponse> createProgram(@RequestBody EventProgramRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createProgram(req));
    }

    @PutMapping("/programs/{id}")
    public ResponseEntity<EventProgramResponse> updateProgram(
            @PathVariable Long id,
            @RequestBody EventProgramRequest req) {
        return ResponseEntity.ok(service.updateProgram(id, req));
    }

    @DeleteMapping("/programs/{id}")
    public ResponseEntity<Void> deleteProgram(@PathVariable Long id) {
        service.deleteProgram(id);
        return ResponseEntity.noContent().build();
    }

    // ── Activity Registration ────────────────────────────────────────────────────

    @PostMapping("/programs/{programId}/register")
    public ResponseEntity<ActivityRegistrationResponse> joinActivity(
            @PathVariable Long programId,
            @AuthenticationPrincipal AppUser user,
            @RequestBody(required = false) ActivityRegistrationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.joinActivity(programId, user, req));
    }

    @DeleteMapping("/programs/{programId}/register")
    public ResponseEntity<Void> leaveActivity(
            @PathVariable Long programId,
            @AuthenticationPrincipal AppUser user) {
        service.leaveActivity(programId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/programs/{programId}/registrations")
    public ResponseEntity<List<ActivityRegistrationResponse>> getActivityRegistrations(
            @PathVariable Long programId) {
        return ResponseEntity.ok(service.getActivityRegistrations(programId));
    }

    // ── Meal Registration ────────────────────────────────────────────────────────

    @PostMapping("/{eventId}/meals")
    public ResponseEntity<MealRegistrationResponse> saveMeals(
            @PathVariable Long eventId,
            @AuthenticationPrincipal AppUser user,
            @RequestBody MealRegistrationRequest req) {
        req.setEventId(eventId);
        return ResponseEntity.ok(service.saveMeals(user, req));
    }

    @GetMapping("/{eventId}/meals")
    public ResponseEntity<MealRegistrationResponse> getUserMeals(
            @PathVariable Long eventId,
            @AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(service.getUserMeals(eventId, user.getId()));
    }

    @GetMapping("/{eventId}/meals/summary")
    public ResponseEntity<MealSummaryResponse> getMealSummary(@PathVariable Long eventId) {
        return ResponseEntity.ok(service.getMealSummary(eventId));
    }
}
