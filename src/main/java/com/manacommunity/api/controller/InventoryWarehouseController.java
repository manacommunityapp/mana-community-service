package com.manacommunity.api.controller;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.service.InventoryWarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryWarehouseController {

    private final InventoryWarehouseService warehouseService;

    // ─── Categories ──────────────────────────────────────────────────────

    @GetMapping("/categories")
    public ResponseEntity<List<InventoryCategoryResponse>> getCategories() {
        return ResponseEntity.ok(warehouseService.getCategories());
    }

    // ─── Locations ───────────────────────────────────────────────────────

    @GetMapping("/locations")
    public ResponseEntity<List<InventoryLocationResponse>> getAllLocations() {
        return ResponseEntity.ok(warehouseService.getAllLocations());
    }

    @GetMapping("/warehouses/{warehouseId}/locations")
    public ResponseEntity<List<InventoryLocationResponse>> getLocationsByWarehouse(
            @PathVariable Long warehouseId) {
        return ResponseEntity.ok(warehouseService.getLocationsByWarehouse(warehouseId));
    }

    @PostMapping("/locations")
    public ResponseEntity<InventoryLocationResponse> createLocation(
            @Valid @RequestBody InventoryLocationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.createLocation(req));
    }

    // ─── Warehouses ──────────────────────────────────────────────────────

    @GetMapping("/warehouses")
    public ResponseEntity<List<InventoryWarehouseResponse>> getAllWarehouses() {
        return ResponseEntity.ok(warehouseService.getAllWarehouses());
    }

    @GetMapping("/warehouses/{id}")
    public ResponseEntity<InventoryWarehouseResponse> getWarehouseById(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.getWarehouseById(id));
    }

    @PostMapping("/warehouses")
    public ResponseEntity<InventoryWarehouseResponse> createWarehouse(
            @Valid @RequestBody InventoryWarehouseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.createWarehouse(req));
    }

    @PutMapping("/warehouses/{id}")
    public ResponseEntity<InventoryWarehouseResponse> updateWarehouse(
            @PathVariable Long id,
            @Valid @RequestBody InventoryWarehouseRequest req) {
        return ResponseEntity.ok(warehouseService.updateWarehouse(id, req));
    }

    @DeleteMapping("/warehouses/{id}")
    public ResponseEntity<Void> deleteWarehouse(@PathVariable Long id) {
        warehouseService.deleteWarehouse(id);
        return ResponseEntity.noContent().build();
    }
}
