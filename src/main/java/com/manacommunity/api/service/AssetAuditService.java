package com.manacommunity.api.service;

import com.manacommunity.api.model.AssetAuditLog;
import com.manacommunity.api.model.InventoryItem;
import com.manacommunity.api.model.ItemStatus;
import com.manacommunity.api.repository.AssetAuditLogRepository;
import com.manacommunity.api.repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetAuditService {

    private final AssetAuditLogRepository assetAuditLogRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public List<AssetAuditLog> getAllAuditLogs() {
        return assetAuditLogRepository.findAll();
    }

    public List<AssetAuditLog> getAuditLogsByAssetId(Long assetId) {
        return assetAuditLogRepository.findByAssetIdOrderByAuditedAtDesc(assetId);
    }

    @Transactional
    public AssetAuditLog recordAudit(Long assetId, String auditedBy, String actualStatus, int expectedQuantity, int actualQuantity, String notes) {
        InventoryItem asset = inventoryItemRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found with id: " + assetId));

        String expectedStatus = asset.getStatus().name();
        
        // Update asset audit fields
        asset.setLastAuditedAt(LocalDateTime.now());
        asset.setAuditVariance(actualQuantity - expectedQuantity);
        
        if (actualStatus != null && !actualStatus.isBlank()) {
            asset.setStatus(ItemStatus.valueOf(actualStatus));
        } else if (actualQuantity <= 0) {
            asset.setStatus(ItemStatus.LOST);
        }
        
        inventoryItemRepository.save(asset);

        AssetAuditLog log = AssetAuditLog.builder()
                .asset(asset)
                .auditedBy(auditedBy)
                .expectedStatus(expectedStatus)
                .actualStatus(asset.getStatus().name())
                .expectedQuantity(expectedQuantity)
                .actualQuantity(actualQuantity)
                .variance(actualQuantity - expectedQuantity)
                .notes(notes)
                .build();

        return assetAuditLogRepository.save(log);
    }
}
