package com.manacommunity.api.delivery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.delivery.dto.*;
import com.manacommunity.api.delivery.service.ExpressDeliveryPartnerService;
import com.manacommunity.api.delivery.service.ExpressPartnerWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/partners/delivery")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Express Entry Partner API", description = "Specialized Domain APIs for Quick-Commerce and Delivery Aggregators (Zomato, Swiggy, Blinkit, Zepto, Amazon)")
public class ExpressDeliveryPartnerController {

    private final ExpressDeliveryPartnerService service;
    private final ExpressPartnerWebhookService webhookService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping("/notify")
    @Operation(
            summary = "Async partner webhook ingestion (Fast-Ack 202)",
            description = "Hardened webhook receiver for Blinkit, Zomato, and Swiggy with HMAC-SHA256 verification, replay window check, per-partner token bucket rate limiting (429 Retry-After), and isolated bulkhead async processing."
    )
    public ResponseEntity<PartnerWebhookAckResponse> notifyWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Partner-ID", required = false) String partnerId,
            @RequestHeader(value = "X-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Event-ID", required = false) String eventId,
            @RequestHeader(value = "X-Signature", required = false) String signature) {

        PartnerWebhookNotifyRequest request;
        try {
            request = objectMapper.readValue(rawPayload, PartnerWebhookNotifyRequest.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Malformed partner webhook JSON payload: " + ex.getMessage());
        }

        Set<ConstraintViolation<PartnerWebhookNotifyRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(violations.iterator().next().getMessage());
        }

        PartnerWebhookAckResponse ack = webhookService.acceptNotification(
                rawPayload, request, partnerId, timestamp, eventId, signature);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ack);
    }

    @ExceptionHandler(ExpressPartnerWebhookService.PartnerRateLimitException.class)
    public ResponseEntity<Map<String, Object>> handlePartnerRateLimit(
            ExpressPartnerWebhookService.PartnerRateLimitException ex) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", "5");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .body(Map.of(
                        "errorCode", ex.getErrorCode(),
                        "message", ex.getMessage(),
                        "status", 429
                ));
    }

    @PostMapping("/express-entry")
    @Operation(summary = "Register rider dispatch", description = "Invoked by delivery platforms when order is out for delivery. Pre-approves rider gate pass.")
    public ResponseEntity<ExpressEntryResponse> registerDispatch(
            @Valid @RequestBody ExpressEntryDispatchRequest request,
            @RequestHeader(value = "X-Partner-ID", required = false) String partnerId,
            @RequestHeader(value = "X-Signature", required = false) String signature) {

        log.info("Incoming Express Entry registration partnerId={} orderId={}", partnerId, request.getOrderId());
        ExpressEntryResponse response = service.registerDispatch(request, partnerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/express-entry/batch")
    @Operation(summary = "Register multi-order courier batch manifest", description = "Aggregates N packages across M distinct flats under one Master Manifest QR code (EXP-B-...) with flat-coalesced resident notifications.")
    public ResponseEntity<ExpressBatchManifestResponse> registerBatchDispatch(
            @Valid @RequestBody ExpressBatchDispatchRequest request,
            @RequestHeader(value = "X-Partner-ID", required = false) String partnerId) {

        log.info("Incoming Express Batch dispatch partnerId={} packages={}", partnerId, request.getPackages().size());
        return ResponseEntity.ok(service.registerBatchDispatch(request, partnerId));
    }

    @PostMapping("/verify")
    @Operation(summary = "Fast gate clearance verification", description = "Invoked by guard scanners or gate kiosks to instantly verify entry tokens (e.g. EXP-7729).")
    public ResponseEntity<ExpressVerifyResponse> verifyEntry(
            @Valid @RequestBody ExpressVerifyRequest request) {

        log.info("Fast gate clearance verification for code={}", request.getEntryCode());
        ExpressVerifyResponse response = service.verifyEntry(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch/verify")
    @Operation(summary = "1-tap bulk batch manifest gate check-in", description = "Clears an entire multi-package courier manifest in a single guard tap.")
    public ResponseEntity<ExpressBatchManifestResponse> verifyBatchManifest(
            @Valid @RequestBody ExpressVerifyRequest request) {

        log.info("1-tap batch manifest verification for code={}", request.getEntryCode());
        return ResponseEntity.ok(service.verifyBatchManifest(request));
    }

    @PostMapping("/checkout")
    @Operation(summary = "Record rider exit", description = "Closes active gate pass when rider departs from the community.")
    public ResponseEntity<ExpressVerifyResponse> checkoutExit(
            @Valid @RequestBody ExpressCheckoutRequest request) {

        log.info("Rider exit checkout identifier={}", request.getIdentifier());
        ExpressVerifyResponse response = service.checkoutExit(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch/checkout")
    @Operation(summary = "Reconcile multi-order batch exit", description = "Marks delivered packages as DELIVERED and undelivered packages as RTO_CARRIED_OUT at gate exit.")
    public ResponseEntity<ExpressBatchManifestResponse> reconcileBatchExit(
            @Valid @RequestBody ExpressBatchCheckoutRequest request) {

        log.info("Batch manifest exit reconciliation manifestCode={}", request.getManifestCode());
        return ResponseEntity.ok(service.reconcileBatchExit(request));
    }

    @GetMapping("/society-rules/{communityId}")
    @Operation(summary = "Query society delivery rules", description = "Provides two-wheeler parking bays, speed limits, and quiet hours to rider navigation apps.")
    public ResponseEntity<SocietyGateRulesResponse> getSocietyGateRules(
            @PathVariable Long communityId) {

        return ResponseEntity.ok(service.getSocietyGateRules(communityId));
    }

    @GetMapping("/active/{communityId}")
    @Operation(summary = "List active express deliveries", description = "Used by security guard consoles to monitor incoming pre-cleared orders.")
    public ResponseEntity<List<ExpressEntryResponse>> getActiveDeliveries(
            @PathVariable Long communityId) {

        return ResponseEntity.ok(service.getActiveDeliveriesForCommunity(communityId));
    }

    @GetMapping("/preferences/{communityId}/{flatNumber}")
    @Operation(summary = "Get resident delivery preferences", description = "Returns auto-approval settings and doorstep instructions.")
    public ResponseEntity<ResidentDeliveryPreferenceDto> getPreferences(
            @PathVariable Long communityId,
            @PathVariable String flatNumber) {

        return ResponseEntity.ok(service.getPreferences(communityId, flatNumber));
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update resident delivery preferences", description = "Updates auto-approval platform toggles and default leave-at-gate setting.")
    public ResponseEntity<ResidentDeliveryPreferenceDto> updatePreferences(
            @RequestBody ResidentDeliveryPreferenceDto dto) {

        return ResponseEntity.ok(service.updatePreferences(dto.getUserId(), dto));
    }
}
