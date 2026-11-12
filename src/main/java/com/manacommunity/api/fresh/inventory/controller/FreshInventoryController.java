package com.manacommunity.api.fresh.inventory.controller;

import com.manacommunity.api.fresh.inventory.dto.*;
import com.manacommunity.api.fresh.inventory.model.WastageCategory;
import com.manacommunity.api.fresh.inventory.service.FreshStockLedgerService;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fresh/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'VENDOR')")
public class FreshInventoryController {

    private final FreshStockLedgerService stockLedgerService;

    @PostMapping("/movement")
    public ResponseEntity<StockMovementDto> recordStockMovement(
            @Valid @RequestBody RecordStockMovementRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal != null && principal.getId() != null) {
            req.setCreatedByUserId(principal.getId());
        }
        return ResponseEntity.ok(stockLedgerService.recordStockMovement(req));
    }

    @GetMapping("/movements/product/{productId}")
    public ResponseEntity<List<StockMovementDto>> getMovementsByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(stockLedgerService.getMovementsByProduct(productId));
    }

    @GetMapping("/movements/batch/{batchId}")
    public ResponseEntity<List<StockMovementDto>> getMovementsByBatch(@PathVariable Long batchId) {
        return ResponseEntity.ok(stockLedgerService.getMovementsByBatch(batchId));
    }

    @PostMapping("/wastage")
    public ResponseEntity<ProductWastageDto> recordWastage(
            @Valid @RequestBody RecordWastageRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal != null && principal.getId() != null) {
            req.setRecordedByUserId(principal.getId());
        }
        return ResponseEntity.ok(stockLedgerService.recordWastage(req));
    }

    @GetMapping("/wastage")
    public ResponseEntity<List<ProductWastageDto>> getAllWastages() {
        return ResponseEntity.ok(stockLedgerService.getAllWastages());
    }

    @GetMapping("/wastage/product/{productId}")
    public ResponseEntity<List<ProductWastageDto>> getWastagesByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(stockLedgerService.getWastagesByProduct(productId));
    }

    @GetMapping("/wastage/category/{category}")
    public ResponseEntity<List<ProductWastageDto>> getWastagesByCategory(@PathVariable WastageCategory category) {
        return ResponseEntity.ok(stockLedgerService.getWastagesByCategory(category));
    }
}
