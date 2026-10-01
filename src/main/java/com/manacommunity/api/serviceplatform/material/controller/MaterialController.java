package com.manacommunity.api.serviceplatform.material.controller;

import com.manacommunity.api.serviceplatform.material.dto.ServiceMaterialDto;
import com.manacommunity.api.serviceplatform.material.dto.WorkOrderMaterialDto;
import com.manacommunity.api.serviceplatform.material.service.MaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/service-platform/materials")
@RequiredArgsConstructor
@Tag(name = "Materials & Equipment", description = "Service spare parts, inventory, and material tracking APIs")
public class MaterialController {

    private final MaterialService materialService;

    @PostMapping
    @Operation(summary = "Add a material/spare part item to provider inventory")
    public ResponseEntity<ServiceMaterialDto> addMaterial(@RequestBody ServiceMaterialDto dto) {
        return new ResponseEntity<>(materialService.addMaterial(dto), HttpStatus.CREATED);
    }

    @GetMapping("/provider/{providerId}")
    @Operation(summary = "Get materials catalog for a provider")
    public ResponseEntity<List<ServiceMaterialDto>> getMaterialsByProvider(@PathVariable Long providerId) {
        return ResponseEntity.ok(materialService.getMaterialsByProvider(providerId));
    }

    @PostMapping("/work-order/{workOrderId}")
    @Operation(summary = "Add consumed material to a work order")
    public ResponseEntity<WorkOrderMaterialDto> addMaterialToWorkOrder(
            @PathVariable Long workOrderId,
            @RequestParam Long materialId,
            @RequestParam int quantity) {
        return new ResponseEntity<>(materialService.addMaterialToWorkOrder(workOrderId, materialId, quantity), HttpStatus.CREATED);
    }

    @GetMapping("/work-order/{workOrderId}")
    @Operation(summary = "Get materials billed to a work order")
    public ResponseEntity<List<WorkOrderMaterialDto>> getMaterialsForWorkOrder(@PathVariable Long workOrderId) {
        return ResponseEntity.ok(materialService.getMaterialsForWorkOrder(workOrderId));
    }
}
