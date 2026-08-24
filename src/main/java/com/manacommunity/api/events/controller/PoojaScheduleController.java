package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.PoojaReserveRequest;
import com.manacommunity.api.events.dto.PoojaReserveResponse;
import com.manacommunity.api.events.dto.PoojaScheduleDto;
import com.manacommunity.api.events.dto.PoojaScheduleRequest;
import com.manacommunity.api.events.enums.PoojaScheduleStatus;
import com.manacommunity.api.events.service.PoojaScheduleService;
import com.manacommunity.api.events.service.PoojaSlotReservationService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events/pooja-schedules")
public class PoojaScheduleController {

    private final PoojaScheduleService scheduleService;
    private final PoojaSlotReservationService reservationService;

    public PoojaScheduleController(PoojaScheduleService scheduleService,
                                   PoojaSlotReservationService reservationService) {
        this.scheduleService = scheduleService;
        this.reservationService = reservationService;
    }

    // ── Public / Member endpoints ──

    /** List all schedules for a pooja, optionally filtered by date. */
    @GetMapping
    public ResponseEntity<List<PoojaScheduleDto>> list(
            @RequestParam Long poojaId,
            @RequestParam(required = false) LocalDate date) {
        if (date != null) {
            return ResponseEntity.ok(scheduleService.getByPoojaAndDate(poojaId, date));
        }
        return ResponseEntity.ok(scheduleService.getByPooja(poojaId));
    }

    /** Dates with at least one open slot. */
    @GetMapping("/available-dates")
    public ResponseEntity<List<LocalDate>> availableDates(@RequestParam Long poojaId) {
        return ResponseEntity.ok(scheduleService.getAvailableDates(poojaId));
    }

    /** Single schedule with live availability. */
    @GetMapping("/{id}")
    public ResponseEntity<PoojaScheduleDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.getById(id));
    }

    /**
     * Reserve a slot.
     * Lock flow: SELECT FOR UPDATE → expire stale → check capacity → insert reservation.
     * Returns a reservationId the client must pass when submitting the registration.
     */
    @PostMapping("/{scheduleId}/reserve")
    public ResponseEntity<PoojaReserveResponse> reserve(
            @PathVariable Long scheduleId,
            @RequestBody PoojaReserveRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = principal != null ? principal.getUser() : null;
        PoojaReserveResponse response = reservationService.reserve(scheduleId, req, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Admin endpoints ──

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<PoojaScheduleDto> create(
            @RequestBody PoojaScheduleRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.createSchedule(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<PoojaScheduleDto> update(
            @PathVariable Long id,
            @RequestBody PoojaScheduleRequest req) {
        return ResponseEntity.ok(scheduleService.updateSchedule(id, req));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN','SUPER_ADMIN') or hasAuthority('Manage Event Forms')")
    public ResponseEntity<PoojaScheduleDto> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        PoojaScheduleStatus status = PoojaScheduleStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(scheduleService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
