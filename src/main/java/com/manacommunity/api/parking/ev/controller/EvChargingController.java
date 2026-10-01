package com.manacommunity.api.parking.ev.controller;

import com.manacommunity.api.parking.ev.dto.*;
import com.manacommunity.api.parking.ev.service.EvChargingService;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/parking/ev")
@RequiredArgsConstructor
public class EvChargingController {

    private final EvChargingService evChargingService;
    private final LoggedInUserService loggedInUserService;

    /**
     * IoT / OCPP Hardware Telemetry Webhook.
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
    public ResponseEntity<EvSessionResponse> startSession(
            @Valid @RequestBody EvStartSessionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        LoggedInUserService.ResolvedUser user = loggedInUserService.resolveContext(principal);
        EvSessionResponse response = evChargingService.startSession(user.user().getId(), user.communityId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Stop EV Charging Session & Settle.
     */
    @PostMapping("/sessions/{sessionId}/stop")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EvSessionResponse> stopSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody EvStopSessionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        LoggedInUserService.ResolvedUser user = loggedInUserService.resolveContext(principal);
        EvSessionResponse response = evChargingService.stopSession(sessionId, user.user().getId(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get real-time status of all community EV stations.
     */
    @GetMapping("/stations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EvStationResponse>> getStations(@AuthenticationPrincipal UserPrincipal principal) {
        LoggedInUserService.ResolvedUser user = loggedInUserService.resolveContext(principal);
        return ResponseEntity.ok(evChargingService.getCommunityStations(user.communityId()));
    }

    /**
     * Get live telemetry snapshot for mobile app dashboard.
     */
    @GetMapping("/stations/{stationId}/live")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EvLiveTelemetryResponse> getLiveTelemetry(@PathVariable Long stationId) {
        return ResponseEntity.ok(evChargingService.getLiveTelemetry(stationId));
    }

    /**
     * Get resident's charging history.
     */
    @GetMapping("/sessions/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EvSessionResponse>> getMySessions(@AuthenticationPrincipal UserPrincipal principal) {
        LoggedInUserService.ResolvedUser user = loggedInUserService.resolveContext(principal);
        return ResponseEntity.ok(evChargingService.getResidentSessions(user.user().getId()));
    }

    /**
     * Admin: Register new EV Station.
     */
    @PostMapping("/stations")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<EvStationResponse> registerStation(@Valid @RequestBody EvStationResponse request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(evChargingService.registerStation(request));
    }
}
