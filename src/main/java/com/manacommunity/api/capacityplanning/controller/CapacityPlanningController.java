package com.manacommunity.api.capacityplanning.controller;

import com.manacommunity.api.capacityplanning.service.CapacityPlanningService;
import com.manacommunity.api.capacityplanning.service.CapacityPlanningService.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/capacity")
@RequiredArgsConstructor
public class CapacityPlanningController {

    private final CapacityPlanningService capacityService;

    @PostMapping("/morning-rush")
    public ResponseEntity<MorningRushEstimate> estimateMorningRush(@RequestBody MorningRushScenario scenario) {
        return ResponseEntity.ok(capacityService.estimateMorningRush(scenario));
    }

    @PostMapping("/billing-blast")
    public ResponseEntity<BillingBlastEstimate> estimateBillingBlast(@RequestBody BillingBlastScenario scenario) {
        return ResponseEntity.ok(capacityService.estimateMonthlyBillingBlast(scenario));
    }
}
