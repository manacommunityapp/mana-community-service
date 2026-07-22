package com.manacommunity.api.controller;

import com.manacommunity.api.model.MaintenanceRecord;
import com.manacommunity.api.service.MaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @GetMapping
    public ResponseEntity<List<MaintenanceRecord>> getAllRecords() {
        return ResponseEntity.ok(maintenanceService.getAllRecords());
    }

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<List<MaintenanceRecord>> getRecordsByAsset(@PathVariable Long assetId) {
        return ResponseEntity.ok(maintenanceService.getRecordsByAssetId(assetId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PostMapping("/asset/{assetId}")
    public ResponseEntity<MaintenanceRecord> createRecord(
            @PathVariable Long assetId,
            @RequestBody MaintenanceRecord record) {
        return ResponseEntity.ok(maintenanceService.createRecord(assetId, record));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<MaintenanceRecord> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return maintenanceService.updateRecordStatus(id, status)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
