package com.manacommunity.api.fresh.inventory.controller;

import com.manacommunity.api.fresh.inventory.dto.*;
import com.manacommunity.api.fresh.inventory.model.WastageCategory;
import com.manacommunity.api.fresh.inventory.service.FreshStockLedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fresh/inventory")
@RequiredArgsConstructor
public class FreshInventoryController {

    private final FreshStockLedgerService stockLedgerService;

    @PostMapping("/movement")
    public ResponseEntity<StockMovementDto> recordStockMovement(@Valid @RequestBody RecordStockMovementRequest req) {
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
    public ResponseEntity<ProductWastageDto> recordWastage(@Valid @RequestBody RecordWastageRequest req) {
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
