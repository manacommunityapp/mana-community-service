package com.manacommunity.api.finops.controller;

import com.manacommunity.api.finops.dto.FinOpsDtos.*;
import com.manacommunity.api.finops.service.CostAwareDeliveryRouterService;
import com.manacommunity.api.finops.service.FinOpsStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finops")
@RequiredArgsConstructor
public class FinOpsController {

    private final CostAwareDeliveryRouterService deliveryRouterService;
    private final FinOpsStorageService storageService;

    @PostMapping("/delivery/dispatch")
    public ResponseEntity<DeliveryRoutePlanDto> dispatchAlert(@RequestBody DispatchAlertRequest request) {
        return ResponseEntity.ok(deliveryRouterService.dispatchAlert(request));
    }

    @PostMapping("/delivery/{alertId}/escalate")
    public ResponseEntity<DeliveryEventDto> escalateAlert(@PathVariable String alertId) {
        return ResponseEntity.ok(deliveryRouterService.escalateNext(alertId));
    }

    @PostMapping("/delivery/{alertId}/ack")
    public ResponseEntity<DeliveryEventDto> ackAlert(
            @PathVariable String alertId,
            @RequestBody AckAlertRequest request) {
        return ResponseEntity.ok(deliveryRouterService.ackAlert(alertId, request));
    }

    @GetMapping("/delivery/summary")
    public ResponseEntity<TelecomFinOpsSummaryDto> getTelecomSummary(@RequestParam long societyId) {
        return ResponseEntity.ok(deliveryRouterService.getSummary(societyId));
    }

    @GetMapping("/delivery/events")
    public ResponseEntity<List<DeliveryEventDto>> listEvents(@RequestParam long societyId) {
        return ResponseEntity.ok(deliveryRouterService.listEvents(societyId));
    }

    @GetMapping("/storage/policies")
    public ResponseEntity<List<StorageTierPolicyDto>> listStoragePolicies() {
        return ResponseEntity.ok(storageService.listPolicies());
    }

    @PostMapping("/storage/compression-audit")
    public ResponseEntity<StorageFinOpsSummaryDto> recordCompressionAudit(@RequestBody RecordCompressionAuditRequest request) {
        return ResponseEntity.ok(storageService.recordCompressionAudit(request));
    }

    @GetMapping("/storage/summary")
    public ResponseEntity<StorageFinOpsSummaryDto> getStorageSummary(@RequestParam long societyId) {
        return ResponseEntity.ok(storageService.getStorageSummary(societyId));
    }
}
