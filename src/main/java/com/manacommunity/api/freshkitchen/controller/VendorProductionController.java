package com.manacommunity.api.freshkitchen.controller;

import com.manacommunity.api.freshkitchen.dto.BatchReleaseRequest;
import com.manacommunity.api.freshkitchen.dto.ProductionBatchDto;
import com.manacommunity.api.freshkitchen.dto.VendorCapacityDto;
import com.manacommunity.api.freshkitchen.dto.VendorDailyManifestDto;
import com.manacommunity.api.freshkitchen.service.FreshKitchenService;
import com.manacommunity.api.freshkitchen.service.VendorProductionCapacityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/vendor", "/vendor"})
@RequiredArgsConstructor
public class VendorProductionController {

    private final VendorProductionCapacityService capacityService;
    private final FreshKitchenService freshKitchenService;

    @GetMapping("/production-capacity")
    public ResponseEntity<List<VendorCapacityDto>> getCapacity(
            @RequestParam(required = false) String vendorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (vendorId != null && !vendorId.isBlank()) {
            return ResponseEntity.ok(capacityService.getCapacityForVendor(vendorId, date));
        }
        return ResponseEntity.ok(capacityService.getCapacityForDate(date));
    }

    @PostMapping("/production-capacity")
    public ResponseEntity<VendorCapacityDto> setCapacity(
            @Valid @RequestBody VendorCapacityDto request) {
        return ResponseEntity.ok(capacityService.setOrUpdateCapacity(request));
    }

    @GetMapping("/production-manifest")
    public ResponseEntity<VendorDailyManifestDto> getManifest(
            @RequestParam String vendorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(capacityService.getVendorDailyManifest(vendorId, date));
    }

    @PostMapping("/batches")
    public ResponseEntity<ProductionBatchDto> createBatch(
            @Valid @RequestBody ProductionBatchDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(freshKitchenService.createProductionBatch(request));
    }

    @PostMapping("/batches/{batchNumber}/release")
    public ResponseEntity<ProductionBatchDto> releaseBatch(
            @PathVariable String batchNumber,
            @Valid @RequestBody BatchReleaseRequest request) {
        return ResponseEntity.ok(freshKitchenService.releaseBatch(batchNumber, request));
    }
}
