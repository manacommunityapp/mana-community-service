package com.manacommunity.api.fresh.processing.controller;

import com.manacommunity.api.fresh.processing.dto.*;
import com.manacommunity.api.fresh.processing.model.BatchStatus;
import com.manacommunity.api.fresh.processing.service.FreshProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fresh/processing")
@RequiredArgsConstructor
public class FreshProcessingController {

    private final FreshProcessingService processingService;

    // ----- Recipes -----

    @PostMapping("/recipes")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<RecipeDto> createRecipe(@RequestBody CreateRecipeRequest req) {
        return ResponseEntity.ok(processingService.createRecipe(req));
    }

    @GetMapping("/recipes")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<RecipeDto>> getRecipesByVendor(@RequestParam Long vendorId) {
        return ResponseEntity.ok(processingService.getRecipesByVendor(vendorId));
    }

    // ----- Batches -----

    @PostMapping("/batches")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<BatchDto> createBatch(@RequestBody CreateBatchRequest req) {
        return ResponseEntity.ok(processingService.createBatch(req));
    }

    @GetMapping("/batches/{batchId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<BatchDto> getBatch(@PathVariable Long batchId) {
        return ResponseEntity.ok(processingService.getBatch(batchId));
    }

    @GetMapping("/batches")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<BatchDto>> getBatches(
            @RequestParam Long vendorId,
            @RequestParam(required = false) BatchStatus status) {
        if (status != null) {
            return ResponseEntity.ok(processingService.getBatchesByVendorAndStatus(vendorId, status));
        }
        return ResponseEntity.ok(processingService.getBatchesByVendor(vendorId));
    }

    @PutMapping("/batches/{batchId}/output")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<BatchDto> recordOutput(
            @PathVariable Long batchId,
            @RequestBody RecordOutputRequest req) {
        return ResponseEntity.ok(processingService.recordOutput(batchId, req));
    }

    // ----- Quality Check -----

    @PostMapping("/batches/{batchId}/quality-checks")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<QualityCheckDto> addQualityCheck(
            @PathVariable Long batchId,
            @RequestBody AddQualityCheckRequest req) {
        return ResponseEntity.ok(processingService.addQualityCheck(batchId, req));
    }

    // ----- Batch Release Gate -----

    @PostMapping("/batches/{batchId}/release")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<BatchReleaseDto> releaseBatch(
            @PathVariable Long batchId,
            @RequestBody BatchReleaseRequest req) {
        return ResponseEntity.ok(processingService.releaseBatch(batchId, req));
    }

    // ----- Temperature Log -----

    @PostMapping("/batches/{batchId}/temperature-logs")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<TemperatureLogDto> logTemperature(
            @PathVariable Long batchId,
            @RequestBody LogTemperatureRequest req) {
        return ResponseEntity.ok(processingService.logTemperature(batchId, req));
    }
}