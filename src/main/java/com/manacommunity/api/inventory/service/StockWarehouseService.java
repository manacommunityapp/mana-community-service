package com.manacommunity.api.inventory.service;

import com.manacommunity.api.inventory.dto.*;
import com.manacommunity.api.inventory.entity.*;
import com.manacommunity.api.inventory.mapper.InventoryMapper;
import com.manacommunity.api.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockWarehouseService {

    private final StockWarehouseRepository warehouseRepository;
    private final StockLocationRepository locationRepository;
    private final StockPickingTypeRepository pickingTypeRepository;
    private final StockPickingRepository pickingRepository;

    @Transactional(readOnly = true)
    public List<WarehouseDto> getAllWarehouses() {
        return warehouseRepository.findAll().stream()
                .map(InventoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WarehouseDto getWarehouseById(Long id) {
        StockWarehouse w = warehouseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found with id: " + id));
        return InventoryMapper.toDto(w);
    }

    @Transactional
    public WarehouseDto createWarehouse(WarehouseDto dto) {
        StockWarehouse w = StockWarehouse.builder()
                .name(dto.getName())
                .code(dto.getCode().toUpperCase())
                .partnerId(dto.getPartnerId())
                .lotStockId(dto.getLotStockId())
                .receptionSteps(dto.getReceptionSteps())
                .deliverySteps(dto.getDeliverySteps())
                .build();
        w = warehouseRepository.save(w);
        return InventoryMapper.toDto(w);
    }

    @Transactional(readOnly = true)
    public List<LocationDto> getAllLocations() {
        return locationRepository.findAll().stream()
                .map(InventoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LocationDto getLocationById(Long id) {
        StockLocation l = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found with id: " + id));
        return InventoryMapper.toDto(l);
    }

    @Transactional
    public LocationDto createLocation(LocationDto dto) {
        StockLocation l = StockLocation.builder()
                .completeName(dto.getCompleteName())
                .usage(dto.getUsage())
                .locationId(dto.getLocationId())
                .barcode(dto.getBarcode())
                .warehouseId(dto.getWarehouseId())
                .build();
        l = locationRepository.save(l);
        return InventoryMapper.toDto(l);
    }

    @Transactional(readOnly = true)
    public List<PickingTypeStatsDto> getPickingTypeStats() {
        List<StockPickingType> types = pickingTypeRepository.findAll();
        List<PickingTypeStatsDto> statsList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (StockPickingType type : types) {
            String whName = warehouseRepository.findById(type.getWarehouseId())
                    .map(StockWarehouse::getName)
                    .orElse("Unknown Warehouse");

            List<StockPicking> pickings = pickingRepository.findByPickingTypeId(type.getId());

            long toProcess = pickings.stream()
                    .filter(p -> p.getState() == PickingState.ready || p.getState() == PickingState.waiting || p.getState() == PickingState.draft)
                    .count();

            long lateTransfers = pickings.stream()
                    .filter(p -> (p.getState() == PickingState.ready || p.getState() == PickingState.waiting) 
                            && p.getScheduledDate().isBefore(now))
                    .count();

            long backorders = pickings.stream()
                    .filter(p -> p.getState() != PickingState.done && p.getState() != PickingState.cancel 
                            && p.getOrigin() != null && p.getOrigin().toLowerCase().contains("backorder"))
                    .count();

            statsList.add(PickingTypeStatsDto.builder()
                    .pickingTypeId(type.getId())
                    .operationType(type.getName())
                    .warehouse(whName)
                    .toProcess(toProcess)
                    .lateTransfers(lateTransfers)
                    .backorders(backorders)
                    .build());
        }

        return statsList;
    }
}
