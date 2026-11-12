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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<VendorCapacityDto>> getCapacity(
            @RequestParam(required = false) String vendorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (vendorId != null && !vendorId.isBlank()) {
            return ResponseEntity.ok(capacityService.getCapacityForVendor(vendorId, date));
        }
        return ResponseEntity.ok(capacityService.getCapacityForDate(date));
    }

    @PostMapping("/production-capacity")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<VendorCapacityDto> setCapacity(
            @Valid @RequestBody VendorCapacityDto request) {
        return ResponseEntity.ok(capacityService.setOrUpdateCapacity(request));
    }

    @GetMapping("/production-manifest")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<VendorDailyManifestDto> getManifest(
            @RequestParam String vendorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(capacityService.getVendorDailyManifest(vendorId, date));
    }

    @GetMapping(value = "/production-manifest/export", produces = "text/csv")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<String> exportManifestCsv(
            @RequestParam String vendorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        String csv = capacityService.exportVendorDailyManifestCsv(vendorId, date);
        LocalDate queryDate = date != null ? date : LocalDate.now();
        String filename = "manifest-" + vendorId + "-" + queryDate + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csv);
    }

    @PostMapping("/batches")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ProductionBatchDto> createBatch(
            @Valid @RequestBody ProductionBatchDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(freshKitchenService.createProductionBatch(request));
    }

    @PostMapping("/batches/{batchNumber}/release")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ProductionBatchDto> releaseBatch(
            @PathVariable String batchNumber,
            @Valid @RequestBody BatchReleaseRequest request) {
        return ResponseEntity.ok(freshKitchenService.releaseBatch(batchNumber, request));
    }
}
