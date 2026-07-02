package com.manacommunity.api.inventory.controller;

import com.manacommunity.api.inventory.dto.*;
import com.manacommunity.api.inventory.service.StockWarehouseService;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.PermissionCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import static com.manacommunity.api.constants.PermissionConstants.VIEW_ADMIN;

@RestController
@RequestMapping("/api/inventory/warehouses")
@RequiredArgsConstructor
public class StockWarehouseController {

    private final StockWarehouseService warehouseService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping
    public ResponseEntity<List<WarehouseDto>> getAllWarehouses(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(warehouseService.getAllWarehouses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseDto> getWarehouseById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(warehouseService.getWarehouseById(id));
    }

    @PostMapping
    public ResponseEntity<WarehouseDto> createWarehouse(
            @RequestBody WarehouseDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(warehouseService.createWarehouse(dto));
    }

    @GetMapping("/locations")
    public ResponseEntity<List<LocationDto>> getAllLocations(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(warehouseService.getAllLocations());
    }

    @PostMapping("/locations")
    public ResponseEntity<LocationDto> createLocation(
            @RequestBody LocationDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(warehouseService.createLocation(dto));
    }

    @GetMapping("/operation-types/stats")
    public ResponseEntity<List<PickingTypeStatsDto>> getPickingTypeStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(warehouseService.getPickingTypeStats());
    }
}
