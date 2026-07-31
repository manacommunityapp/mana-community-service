package com.manacommunity.api.service;

import com.manacommunity.api.model.InventoryItem;
import com.manacommunity.api.model.ItemStatus;
import com.manacommunity.api.model.MaintenanceRecord;
import com.manacommunity.api.repository.InventoryItemRepository;
import com.manacommunity.api.repository.MaintenanceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service("inventoryMaintenanceService")
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public List<MaintenanceRecord> getAllRecords() {
        return maintenanceRecordRepository.findAll();
    }

    public List<MaintenanceRecord> getRecordsByAssetId(Long assetId) {
        return maintenanceRecordRepository.findByAssetIdOrderByMaintenanceDateDesc(assetId);
    }

    @Transactional
    public MaintenanceRecord createRecord(Long assetId, MaintenanceRecord record) {
        InventoryItem asset = inventoryItemRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found with id: " + assetId));
        record.setAsset(asset);
        
        // Update asset status based on maintenance status
        if ("IN_PROGRESS".equalsIgnoreCase(record.getStatus()) || "SCHEDULED".equalsIgnoreCase(record.getStatus())) {
            asset.setStatus(ItemStatus.MAINTENANCE);
        } else if ("COMPLETED".equalsIgnoreCase(record.getStatus())) {
            asset.setStatus(ItemStatus.AVAILABLE);
            if (record.getMaintenanceDate() != null) {
                asset.setNextMaintenanceDueAt(record.getMaintenanceDate().plusMonths(6));
            }
        }
        inventoryItemRepository.save(asset);
        
        return maintenanceRecordRepository.save(record);
    }

    @Transactional
    public Optional<MaintenanceRecord> updateRecordStatus(Long id, String status) {
        return maintenanceRecordRepository.findById(id).map(record -> {
            record.setStatus(status);
            InventoryItem asset = record.getAsset();
            if ("COMPLETED".equalsIgnoreCase(status)) {
                asset.setStatus(ItemStatus.AVAILABLE);
                if (record.getMaintenanceDate() != null) {
                    asset.setNextMaintenanceDueAt(record.getMaintenanceDate().plusMonths(6));
                }
            } else if ("IN_PROGRESS".equalsIgnoreCase(status) || "SCHEDULED".equalsIgnoreCase(status)) {
                asset.setStatus(ItemStatus.MAINTENANCE);
            }
            inventoryItemRepository.save(asset);
            return maintenanceRecordRepository.save(record);
        });
    }
}
