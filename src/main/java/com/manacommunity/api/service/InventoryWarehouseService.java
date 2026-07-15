package com.manacommunity.api.service;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.InventoryCategory;
import com.manacommunity.api.model.InventoryLocation;
import com.manacommunity.api.model.InventoryWarehouse;
import com.manacommunity.api.repository.InventoryCategoryRepository;
import com.manacommunity.api.repository.InventoryLocationRepository;
import com.manacommunity.api.repository.InventoryWarehouseRepository;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryWarehouseService {

    private final InventoryWarehouseRepository warehouseRepo;
    private final InventoryLocationRepository locationRepo;
    private final InventoryCategoryRepository categoryRepo;
    private final AuditService auditService;

    // ─── Categories ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<InventoryCategoryResponse> getCategories() {
        return categoryRepo.findAll().stream().map(c ->
            new InventoryCategoryResponse(c.getId(), c.getName(), c.getParent() != null ? c.getParent().getId() : null)
        ).toList();
    }

    // ─── Locations ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<InventoryLocationResponse> getAllLocations() {
        return locationRepo.findByIsActiveTrueOrderByCompleteNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryLocationResponse> getLocationsByWarehouse(Long warehouseId) {
        return locationRepo.findByWarehouseIdAndIsActiveTrueOrderByCompleteNameAsc(warehouseId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InventoryLocationResponse createLocation(InventoryLocationRequest req) {
        InventoryWarehouse wh = null;
        if (req.warehouseId() != null) {
            wh = warehouseRepo.findById(req.warehouseId())
                    .orElseThrow(() -> new ResourceNotFoundException("InventoryWarehouse", req.warehouseId()));
        }

        InventoryLocation loc = InventoryLocation.builder()
                .completeName(req.completeName())
                .usage(InventoryLocation.LocationUsage.valueOf(req.usage()))
                .barcode(req.barcode())
                .warehouse(wh)
                .build();

        InventoryLocation saved = locationRepo.save(loc);
        log.info("Created location: {}", saved.getCompleteName());

        auditService.record(AuditAction.CONFIG_UPDATED, AuditModule.ADMIN,
                "InventoryLocation", String.valueOf(saved.getId()), null,
                "name=" + saved.getCompleteName() + ", usage=" + saved.getUsage());

        return toResponse(saved);
    }

    // ─── Warehouse CRUD ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<InventoryWarehouseResponse> getAllWarehouses() {
        return warehouseRepo.findByIsActiveTrueOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryWarehouseResponse getWarehouseById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public InventoryWarehouseResponse createWarehouse(InventoryWarehouseRequest req) {
        InventoryWarehouse wh = InventoryWarehouse.builder()
                .name(req.name())
                .code(req.code())
                .receptionSteps(req.receptionSteps() != null ? InventoryWarehouse.StepType.valueOf(req.receptionSteps()) : InventoryWarehouse.StepType.ONE_STEP)
                .deliverySteps(req.deliverySteps() != null ? InventoryWarehouse.StepType.valueOf(req.deliverySteps()) : InventoryWarehouse.StepType.ONE_STEP)
                .build();

        InventoryWarehouse saved = warehouseRepo.save(wh);
        log.info("Created warehouse: code={}, name={}", saved.getCode(), saved.getName());

        // Create standard locations (Stock, Input, Output)
        createDefaultLocation(saved.getName() + "/Stock", InventoryLocation.LocationUsage.INTERNAL, saved);
        createDefaultLocation(saved.getName() + "/Input", InventoryLocation.LocationUsage.INTERNAL, saved);
        createDefaultLocation(saved.getName() + "/Output", InventoryLocation.LocationUsage.INTERNAL, saved);

        auditService.record(AuditAction.CONFIG_UPDATED, AuditModule.ADMIN,
                "InventoryWarehouse", String.valueOf(saved.getId()), null,
                "name=" + saved.getName() + ", code=" + saved.getCode());

        return toResponse(saved);
    }

    @Transactional
    public InventoryWarehouseResponse updateWarehouse(Long id, InventoryWarehouseRequest req) {
        InventoryWarehouse wh = findOrThrow(id);
        String oldValue = "name=" + wh.getName() + ", code=" + wh.getCode() + ", rec=" + wh.getReceptionSteps();

        wh.setName(req.name());
        wh.setCode(req.code());
        if (req.receptionSteps() != null) wh.setReceptionSteps(InventoryWarehouse.StepType.valueOf(req.receptionSteps()));
        if (req.deliverySteps() != null) wh.setDeliverySteps(InventoryWarehouse.StepType.valueOf(req.deliverySteps()));

        InventoryWarehouse saved = warehouseRepo.save(wh);
        String newValue = "name=" + saved.getName() + ", code=" + saved.getCode() + ", rec=" + saved.getReceptionSteps();

        auditService.record(AuditAction.CONFIG_UPDATED, AuditModule.ADMIN,
                "InventoryWarehouse", String.valueOf(id), oldValue, newValue);

        return toResponse(saved);
    }

    @Transactional
    public void deleteWarehouse(Long id) {
        InventoryWarehouse wh = findOrThrow(id);
        wh.setIsActive(false);
        warehouseRepo.save(wh);

        auditService.record(AuditAction.CONFIG_UPDATED, AuditModule.ADMIN,
                "InventoryWarehouse", String.valueOf(id), "active=true", "active=false");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private void createDefaultLocation(String completeName, InventoryLocation.LocationUsage usage, InventoryWarehouse wh) {
        InventoryLocation loc = InventoryLocation.builder()
                .completeName(completeName)
                .usage(usage)
                .warehouse(wh)
                .build();
        locationRepo.save(loc);
    }

    private InventoryWarehouse findOrThrow(Long id) {
        return warehouseRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryWarehouse", id));
    }

    private InventoryWarehouseResponse toResponse(InventoryWarehouse w) {
        Long lotStockId = w.getLocations().stream()
                .filter(l -> l.getCompleteName().endsWith("/Stock"))
                .map(InventoryLocation::getId)
                .findFirst().orElse(null);

        return new InventoryWarehouseResponse(
                w.getId(), w.getName(), w.getCode(),
                w.getCommunity() != null ? w.getCommunity().getId() : null,
                lotStockId,
                w.getReceptionSteps().name(),
                w.getDeliverySteps().name()
        );
    }

    private InventoryLocationResponse toResponse(InventoryLocation l) {
        return new InventoryLocationResponse(
                l.getId(), l.getCompleteName(), l.getUsage().name(), l.getBarcode(),
                l.getWarehouse() != null ? l.getWarehouse().getId() : null
        );
    }
}
