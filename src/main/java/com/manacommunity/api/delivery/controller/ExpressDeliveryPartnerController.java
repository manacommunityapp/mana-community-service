package com.manacommunity.api.delivery.controller;

import com.manacommunity.api.delivery.dto.*;
import com.manacommunity.api.delivery.service.ExpressDeliveryPartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/partners/delivery")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Express Entry Partner API", description = "Specialized Domain APIs for Quick-Commerce and Delivery Aggregators (Zomato, Swiggy, Blinkit, Zepto, Amazon)")
public class ExpressDeliveryPartnerController {

    private final ExpressDeliveryPartnerService service;

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

    @PostMapping("/verify")
    @Operation(summary = "Fast gate clearance verification", description = "Invoked by guard scanners or gate kiosks to instantly verify entry tokens (e.g. EXP-7729).")
    public ResponseEntity<ExpressVerifyResponse> verifyEntry(
            @Valid @RequestBody ExpressVerifyRequest request) {

        log.info("Fast gate clearance verification for code={}", request.getEntryCode());
        ExpressVerifyResponse response = service.verifyEntry(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout")
    @Operation(summary = "Record rider exit", description = "Closes active gate pass when rider departs from the community.")
    public ResponseEntity<ExpressVerifyResponse> checkoutExit(
            @Valid @RequestBody ExpressCheckoutRequest request) {

        log.info("Rider exit checkout identifier={}", request.getIdentifier());
        ExpressVerifyResponse response = service.checkoutExit(request);
        return ResponseEntity.ok(response);
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
