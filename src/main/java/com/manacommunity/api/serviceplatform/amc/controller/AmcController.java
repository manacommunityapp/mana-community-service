package com.manacommunity.api.serviceplatform.amc.controller;

import com.manacommunity.api.serviceplatform.amc.dto.AmcPlanDto;
import com.manacommunity.api.serviceplatform.amc.dto.AmcSubscriptionDto;
import com.manacommunity.api.serviceplatform.amc.dto.ServiceWarrantyDto;
import com.manacommunity.api.serviceplatform.amc.service.AmcService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/service-platform/amc")
@RequiredArgsConstructor
@Tag(name = "AMC & Warranty", description = "Annual maintenance contract and service warranty APIs")
public class AmcController {

    private final AmcService amcService;

    @PostMapping("/plans")
    @Operation(summary = "Create an AMC plan")
    public ResponseEntity<AmcPlanDto> createPlan(@RequestBody AmcPlanDto dto) {
        return new ResponseEntity<>(amcService.createPlan(dto), HttpStatus.CREATED);
    }

    @GetMapping("/plans/provider/{providerId}")
    @Operation(summary = "Get active AMC plans for a provider")
    public ResponseEntity<List<AmcPlanDto>> getPlansByProvider(@PathVariable Long providerId) {
        return ResponseEntity.ok(amcService.getPlansByProvider(providerId));
    }

    @PostMapping("/subscribe")
    @Operation(summary = "Subscribe to an AMC plan")
    public ResponseEntity<AmcSubscriptionDto> subscribe(
            @RequestParam Long planId,
            @RequestParam Long userId,
            @RequestParam Long communityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        return new ResponseEntity<>(amcService.subscribe(planId, userId, communityId, startDate), HttpStatus.CREATED);
    }

    @GetMapping("/subscriptions/user/{userId}")
    @Operation(summary = "Get user subscriptions")
    public ResponseEntity<List<AmcSubscriptionDto>> getUserSubscriptions(@PathVariable Long userId) {
        return ResponseEntity.ok(amcService.getUserSubscriptions(userId));
    }

    @PostMapping("/subscriptions/{subscriptionId}/use-visit")
    @Operation(summary = "Consume a visit from an AMC subscription")
    public ResponseEntity<AmcSubscriptionDto> useVisit(@PathVariable Long subscriptionId) {
        return ResponseEntity.ok(amcService.useVisit(subscriptionId));
    }

    @PostMapping("/warranties")
    @Operation(summary = "Issue a warranty for a completed work order")
    public ResponseEntity<ServiceWarrantyDto> createWarranty(
            @RequestParam Long workOrderId,
            @RequestParam int warrantyDays,
            @RequestParam(required = false) String terms) {
        return new ResponseEntity<>(amcService.createWarrantyForWorkOrder(workOrderId, warrantyDays, terms), HttpStatus.CREATED);
    }

    @GetMapping("/warranties/work-order/{workOrderId}")
    @Operation(summary = "Get warranty details for a work order")
    public ResponseEntity<ServiceWarrantyDto> getWarrantyByWorkOrder(@PathVariable Long workOrderId) {
        return ResponseEntity.ok(amcService.getWarrantyByWorkOrder(workOrderId));
    }
}
