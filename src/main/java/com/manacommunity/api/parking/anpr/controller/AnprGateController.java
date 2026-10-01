package com.manacommunity.api.parking.anpr.controller;

import com.manacommunity.api.parking.anpr.dto.*;
import com.manacommunity.api.parking.anpr.service.AnprGateService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ANPR Gate REST API.
 *
 * Webhook path (called by mana-community-anpr-services Python microservice):
 *   POST /api/parking/anpr/webhook
 *
 * Security dashboard paths (called by front-end admin panel):
 *   GET  /api/parking/anpr/events
 *   GET  /api/parking/anpr/events?gateId=GATE_MAIN_IN
 *   GET  /api/parking/anpr/plates/{plate}/history
 *   GET  /api/parking/anpr/alerts/pending
 *   GET  /api/parking/anpr/summary
 */
@RestController
@RequestMapping("/api/parking/anpr")
@RequiredArgsConstructor
@Slf4j
public class AnprGateController {

    private final AnprGateService anprGateService;
    private final LoggedInUserService loggedInUserService;

    // ── Webhook (called by ANPR Python service) ───────────────────────────────

    /**
     * POST /api/parking/anpr/webhook
     *
     * Receives a plate recognition event from mana-community-anpr-services.
     * Secured via X-ANPR-API-Key header (validated by Spring Security filter or @RequestHeader check).
     *
     * Returns the barrier action decision (OPEN / HOLD / DENY) so the Python service
     * can forward it to the barrier controller.
     */
    @PostMapping("/webhook")
    public ResponseEntity<AnprWebhookResponse> handleWebhook(
            @RequestHeader(value = "X-ANPR-API-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Community-Id") Long communityId,
            @RequestBody AnprWebhookPayload payload
    ) {
        // TODO: validate apiKey against configured secret (application.properties)
        log.info("ANPR webhook received: plate={} gate={} confidence={} communityId={}",
                payload.plateNumber(), payload.gateId(), payload.confidence(), communityId);

        AnprWebhookResponse response = anprGateService.processWebhook(communityId, payload);
        return ResponseEntity.ok(response);
    }

    // ── Security dashboard APIs ────────────────────────────────────────────────

    /** GET /api/parking/anpr/events — paginated gate event list. */
    @GetMapping("/events")
    public ResponseEntity<Page<AnprGateEventResponse>> getEvents(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String gateId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(anprGateService.getEvents(communityId, gateId, page, size));
    }

    /** GET /api/parking/anpr/plates/{plate}/history — full event trail for a plate. */
    @GetMapping("/plates/{plate}/history")
    public ResponseEntity<List<AnprGateEventResponse>> getPlateHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String plate
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(anprGateService.getPlateHistory(user.getCommunity().getId(), plate));
    }

    /** GET /api/parking/anpr/alerts/pending — unknown vehicle alerts since midnight. */
    @GetMapping("/alerts/pending")
    public ResponseEntity<List<AnprGateEventResponse>> getPendingAlerts(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(anprGateService.getPendingAlerts(user.getCommunity().getId()));
    }

    /** GET /api/parking/anpr/summary — today's OPEN/HOLD/DENY counts. */
    @GetMapping("/summary")
    public ResponseEntity<AnprGateSummaryResponse> getDailySummary(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(anprGateService.getDailySummary(user.getCommunity().getId()));
    }
}
