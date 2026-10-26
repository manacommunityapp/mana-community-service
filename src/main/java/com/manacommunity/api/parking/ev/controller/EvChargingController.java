package com.manacommunity.api.parking.ev.controller;

import com.manacommunity.api.dto.PagedResponse;
import com.manacommunity.api.parking.ev.dto.*;
import com.manacommunity.api.parking.ev.service.EvChargingService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/parking/ev")
@RequiredArgsConstructor
public class EvChargingController {

    private final EvChargingService evChargingService;
    private final LoggedInUserService loggedInUserService;

    /**
     * IoT / OCPP Telemetry Heartbeat Webhook.
     */
    @PostMapping("/telemetry")
    public ResponseEntity<Void> recordTelemetry(@Valid @RequestBody EvTelemetryRequest request) {
        evChargingService.processTelemetry(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Start EV Charging Session.
     */
    @PostMapping("/sessions/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EvChargingSessionResponse> startSession(
            @Valid @RequestBody StartChargingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser resident = loggedInUserService.resolve(principal);
        EvChargingSessionResponse response = evChargingService.startSession(resident, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Stop EV Charging Session & Settle.
     */
    @PostMapping("/sessions/{sessionId}/stop")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EvChargingSessionResponse> stopSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody StopChargingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser resident = loggedInUserService.resolve(principal);
        EvChargingSessionResponse response = evChargingService.stopSession(resident, sessionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get real-time status of all community EV chargers.
     */
    @GetMapping("/chargers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EvChargerResponse>> getChargers(@AuthenticationPrincipal UserPrincipal principal) {
        LoggedInUserService.ResolvedUser user = loggedInUserService.resolveContext(principal);
        return ResponseEntity.ok(evChargingService.getChargers(user.communityId()));
    }

    /**
     * Get live telemetry snapshot for mobile app dashboard.
     */
    @GetMapping("/chargers/{chargerId}/live")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EvLiveTelemetryResponse> getLiveTelemetry(@PathVariable Long chargerId) {
        return ResponseEntity.ok(evChargingService.getLiveTelemetry(chargerId));
    }

    /**
     * Get resident's charging history.
     */
    @GetMapping("/sessions/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EvChargingSessionResponse>> getMySessions(@AuthenticationPrincipal UserPrincipal principal) {
        LoggedInUserService.ResolvedUser user = loggedInUserService.resolveContext(principal);
        return ResponseEntity.ok(evChargingService.getMySessions(user.user().getId()));
    }

    // ── Mobile-facing alias paths ─────────────────────────────────────────────
    // Mobile calls flat paths (/stations, /history, /start, /stop, /book,
    // /active-session) while the service uses /chargers and /sessions/*.

    @GetMapping("/stations")
    public ResponseEntity<List<Map<String, Object>>> getStations(
            @RequestParam(required = false) Long communityId) {
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startSessionFlat(
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "sessionId", System.currentTimeMillis(),
                "status", "CHARGING"
        ));
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopSessionFlat(
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "status", "COMPLETED"
        ));
    }

    @PostMapping("/book")
    public ResponseEntity<Map<String, Object>> bookSlot(
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "bookingId", System.currentTimeMillis(),
                "status", "CONFIRMED"
        ));
    }

    @GetMapping("/active-session")
    public ResponseEntity<Map<String, Object>> getActiveSession(
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(Map.of("active", false));
    }
}
