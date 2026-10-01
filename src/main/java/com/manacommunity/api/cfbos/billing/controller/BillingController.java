package com.manacommunity.api.cfbos.billing.controller;
import com.manacommunity.api.cfbos.billing.dto.*;
import com.manacommunity.api.cfbos.billing.entity.*;
import com.manacommunity.api.cfbos.billing.service.BillingService;
import com.manacommunity.api.cfbos.charge.dto.PropertyContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cfbos/billing")
@RequiredArgsConstructor
public class BillingController {
    private final BillingService billingService;

    @PostMapping("/runs")
    public ResponseEntity<BillingRunResponse> executeBillingRun(
            @RequestBody BillingRunRequest request,
            @RequestParam(required = false) List<PropertyContext> properties) {
        List<PropertyContext> props = properties != null ? properties : List.of();
        return ResponseEntity.ok(billingService.runBilling(request, props));
    }

    @GetMapping("/runs/{id}")
    public ResponseEntity<BillingRunResponse> getBillingRun(@PathVariable Long id) {
        return ResponseEntity.ok(billingService.getBillingRunById(id));
    }

    @GetMapping("/runs")
    public ResponseEntity<List<BillingRunResponse>> getAllRuns() {
        return ResponseEntity.ok(billingService.getAllBillingRuns());
    }

    @GetMapping("/schedules")
    public ResponseEntity<List<BillingSchedule>> getSchedules() {
        return ResponseEntity.ok(billingService.getAllSchedules());
    }

    @GetMapping("/charge-heads")
    public ResponseEntity<List<ChargeHead>> getChargeHeads() {
        return ResponseEntity.ok(billingService.getAllChargeHeads());
    }

    @GetMapping("/charge-types")
    public ResponseEntity<List<ChargeType>> getChargeTypes() {
        return ResponseEntity.ok(billingService.getAllChargeTypes());
    }
}
