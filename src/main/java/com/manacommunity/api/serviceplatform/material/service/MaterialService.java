package com.manacommunity.api.serviceplatform.material.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.entity.WorkOrder;
import com.manacommunity.api.serviceplatform.material.dto.ServiceMaterialDto;
import com.manacommunity.api.serviceplatform.material.dto.WorkOrderMaterialDto;
import com.manacommunity.api.serviceplatform.material.engine.MaterialEngine;
import com.manacommunity.api.serviceplatform.material.entity.ServiceMaterial;
import com.manacommunity.api.serviceplatform.material.entity.WorkOrderMaterial;
import com.manacommunity.api.serviceplatform.material.repository.ServiceMaterialRepository;
import com.manacommunity.api.serviceplatform.material.repository.WorkOrderMaterialRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceProviderRepository;
import com.manacommunity.api.serviceplatform.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialService {

    private final ServiceMaterialRepository materialRepository;
    private final WorkOrderMaterialRepository workOrderMaterialRepository;
    private final ServiceProviderRepository providerRepository;
    private final WorkOrderRepository workOrderRepository;
    private final MaterialEngine engine;

    @Transactional
    public ServiceMaterialDto addMaterial(ServiceMaterialDto dto) {
        ServiceProvider provider = providerRepository.findById(dto.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", dto.getProviderId()));

        ServiceMaterial material = ServiceMaterial.builder()
                .provider(provider)
                .itemName(dto.getItemName())
                .itemCode(dto.getItemCode())
                .unitOfMeasure(dto.getUnitOfMeasure())
                .unitPrice(dto.getUnitPrice())
                .stockQuantity(dto.getStockQuantity())
                .active(true)
                .build();

        ServiceMaterial saved = materialRepository.save(material);
        return mapMaterialToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ServiceMaterialDto> getMaterialsByProvider(Long providerId) {
        return materialRepository.findByProviderIdAndActiveTrue(providerId).stream()
                .map(this::mapMaterialToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkOrderMaterialDto addMaterialToWorkOrder(Long workOrderId, Long materialId, int quantity) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", workOrderId));
        ServiceMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceMaterial", materialId));

        engine.deductStock(material, quantity);
        materialRepository.save(material);

        BigDecimal totalPrice = engine.calculateMaterialTotal(material, quantity);

        WorkOrderMaterial wom = WorkOrderMaterial.builder()
                .workOrder(workOrder)
                .material(material)
                .quantity(quantity)
                .unitPrice(material.getUnitPrice())
                .totalPrice(totalPrice)
                .build();

        WorkOrderMaterial saved = workOrderMaterialRepository.save(wom);
        return mapWomToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<WorkOrderMaterialDto> getMaterialsForWorkOrder(Long workOrderId) {
        return workOrderMaterialRepository.findByWorkOrderId(workOrderId).stream()
                .map(this::mapWomToDto)
                .collect(Collectors.toList());
    }

    private ServiceMaterialDto mapMaterialToDto(ServiceMaterial m) {
        return ServiceMaterialDto.builder()
                .id(m.getId())
                .providerId(m.getProvider().getId())
                .itemName(m.getItemName())
                .itemCode(m.getItemCode())
                .unitOfMeasure(m.getUnitOfMeasure())
                .unitPrice(m.getUnitPrice())
                .stockQuantity(m.getStockQuantity())
                .active(m.isActive())
                .build();
    }

    private WorkOrderMaterialDto mapWomToDto(WorkOrderMaterial wom) {
        return WorkOrderMaterialDto.builder()
                .id(wom.getId())
                .workOrderId(wom.getWorkOrder().getId())
                .materialId(wom.getMaterial().getId())
                .materialName(wom.getMaterial().getItemName())
                .quantity(wom.getQuantity())
                .unitPrice(wom.getUnitPrice())
                .totalPrice(wom.getTotalPrice())
                .build();
    }
}
