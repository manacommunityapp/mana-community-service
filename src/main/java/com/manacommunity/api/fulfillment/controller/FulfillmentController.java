package com.manacommunity.api.fulfillment.controller;

import com.manacommunity.api.fulfillment.dto.FulfillmentDtos.*;
import com.manacommunity.api.fulfillment.service.FulfillmentService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/fulfillment", "/api/v1/fulfillment"})
@RequiredArgsConstructor
public class FulfillmentController {

    private final FulfillmentService fulfillmentService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/{orderId}/timeline")
    public ResponseEntity<List<FulfillmentEventResponse>> getTimeline(
            @PathVariable String orderId) {
        return ResponseEntity.ok(fulfillmentService.getTimeline(orderId));
    }

    @GetMapping("/{orderId}/status")
    public ResponseEntity<Map<String, String>> getCurrentStatus(
            @PathVariable String orderId) {
        return ResponseEntity.ok(Map.of("status", fulfillmentService.getCurrentStatus(orderId)));
    }

    @PostMapping("/{orderId}/event")
    public ResponseEntity<FulfillmentEventResponse> recordEvent(
            @PathVariable String orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RecordEventRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(fulfillmentService.recordEvent(
                orderId, req.orderType(), communityId,
                req.eventType(), user.getId(), req.actorRole(), req.note()));
    }

    @PostMapping("/{orderId}/assign-delivery")
    public ResponseEntity<DeliveryStatusResponse> assignDelivery(
            @PathVariable String orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AssignDeliveryRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(fulfillmentService.assignDelivery(
                orderId, communityId,
                req.deliveryPersonId(), req.deliveryPersonName(),
                req.deliveryPersonPhone(), req.estimatedArrival()));
    }
}
