package com.manacommunity.api.inventory.service;

import com.manacommunity.api.inventory.dto.*;
import com.manacommunity.api.inventory.entity.*;
import com.manacommunity.api.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockReportingService {

    private final StockQuantRepository quantRepository;
    private final StockMoveLineRepository moveLineRepository;
    private final ProductRepository productRepository;
    private final StockLocationRepository locationRepository;
    private final StockWarehouseRepository warehouseRepository;
    private final StockPickingRepository pickingRepository;
    private final StockLotRepository lotRepository;

    @Transactional(readOnly = true)
    public List<StockLevelReportDto> getStockLevelReport() {
        List<StockQuant> quants = quantRepository.findAll();
        List<StockLevelReportDto> report = new ArrayList<>();

        for (StockQuant q : quants) {
            if (q.getQuantity() <= 0) continue;

            Product p = productRepository.findById(q.getProductId()).orElse(null);
            StockLocation loc = locationRepository.findById(q.getLocationId()).orElse(null);
            if (p == null || loc == null) continue;

            String whName = "Virtual Locations";
            if (loc.getWarehouseId() != null) {
                whName = warehouseRepository.findById(loc.getWarehouseId())
                        .map(StockWarehouse::getName)
                        .orElse("Unknown Warehouse");
            }

            report.add(StockLevelReportDto.builder()
                    .warehouse(whName)
                    .location(loc.getCompleteName())
                    .productName(p.getName())
                    .internalReference(p.getSku())
                    .onHand(q.getQuantity())
                    .reserved(q.getReservedQuantity())
                    .available(q.getQuantity() - q.getReservedQuantity())
                    .build());
        }
        return report;
    }

    @Transactional(readOnly = true)
    public List<MoveHistoryReportDto> getMoveHistoryReport() {
        List<StockMoveLine> lines = moveLineRepository.findAll();
        List<MoveHistoryReportDto> report = new ArrayList<>();

        for (StockMoveLine ml : lines) {
            Product p = productRepository.findById(ml.getProductId()).orElse(null);
            StockLocation src = locationRepository.findById(ml.getLocationId()).orElse(null);
            StockLocation dest = locationRepository.findById(ml.getLocationDestId()).orElse(null);
            if (p == null || src == null || dest == null) continue;

            String pickingName = "Manual Adjustment";
            String originDoc = "Inventory Sync";
            if (ml.getPickingId() != null) {
                Optional<StockPicking> optPicking = pickingRepository.findById(ml.getPickingId());
                pickingName = optPicking.map(StockPicking::getName).orElse("Manual Adjustment");
                originDoc = optPicking.map(StockPicking::getOrigin).orElse("Inventory Sync");
            }

            String lotName = ml.getLotId() != null ? lotRepository.findById(ml.getLotId())
                    .map(StockLot::getName)
                    .orElse("N/A") : "N/A";

            report.add(MoveHistoryReportDto.builder()
                    .transferReference(pickingName)
                    .sourceDocument(originDoc)
                    .executionDate(ml.getCreatedAt())
                    .product(p.getName())
                    .fromLocation(src.getCompleteName())
                    .toLocation(dest.getCompleteName())
                    .qtyDone(ml.getQuantity())
                    .serialLotNumber(lotName)
                    .build());
        }
        return report;
    }
}
