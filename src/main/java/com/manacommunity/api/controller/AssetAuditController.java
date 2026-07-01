package com.manacommunity.api.controller;

import com.manacommunity.api.model.AssetAuditLog;
import com.manacommunity.api.service.AssetAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/audit")
@RequiredArgsConstructor
public class AssetAuditController {

    private final AssetAuditService assetAuditService;

    @GetMapping
    public ResponseEntity<List<AssetAuditLog>> getAllLogs() {
        return ResponseEntity.ok(assetAuditService.getAllAuditLogs());
    }

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<List<AssetAuditLog>> getLogsByAsset(@PathVariable Long assetId) {
        return ResponseEntity.ok(assetAuditService.getAuditLogsByAssetId(assetId));
    }

    @PostMapping("/asset/{assetId}")
    public ResponseEntity<AssetAuditLog> recordAudit(
            @PathVariable Long assetId,
            @RequestParam String auditedBy,
            @RequestParam(required = false) String actualStatus,
            @RequestParam int expectedQuantity,
            @RequestParam int actualQuantity,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(assetAuditService.recordAudit(assetId, auditedBy, actualStatus, expectedQuantity, actualQuantity, notes));
    }
}
