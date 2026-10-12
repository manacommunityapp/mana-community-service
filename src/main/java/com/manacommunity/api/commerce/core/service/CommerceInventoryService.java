package com.manacommunity.api.commerce.core.service;

import com.manacommunity.api.commerce.core.model.CommerceInventory;
import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.commerce.core.model.CommerceOrderItem;
import com.manacommunity.api.commerce.core.repository.CommerceInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages stock lifecycle for CommerceInventory records.
 *
 * Flow:
 *  1. reserveStock(order)   — called at checkout; moves availableQty → reservedQty
 *  2. commitStock(order)    — called on payment confirmation; moves reservedQty → committedQty
 *  3. releaseStock(order)   — called on cancellation; returns reservedQty (or committed) → availableQty
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommerceInventoryService {

    private final CommerceInventoryRepository inventoryRepository;

    @Transactional
    public void reserveStock(CommerceOrder order) {
        applyToItems(order.getItems(), "RESERVE");
    }

    @Transactional
    public void commitStock(CommerceOrder order) {
        applyToItems(order.getItems(), "COMMIT");
    }

    @Transactional
    public void releaseStock(CommerceOrder order) {
        applyToItems(order.getItems(), "RELEASE");
    }

    private void applyToItems(List<CommerceOrderItem> items, String action) {
        if (items == null) return;
        for (CommerceOrderItem item : items) {
            if (item.getSku() == null || item.getSku().isBlank()) continue;
            try {
                java.util.Optional<CommerceInventory> opt = inventoryRepository.findBySkuForUpdate(item.getSku());
                if (opt.isEmpty()) continue;
                CommerceInventory inv = opt.get();
                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                switch (action) {
                    case "RESERVE" -> reserve(inv, qty, item.getSku());
                    case "COMMIT"  -> commit(inv, qty, item.getSku());
                    case "RELEASE" -> release(inv, qty, item.getSku());
                }
                inv.setUpdatedAt(LocalDateTime.now());
                inventoryRepository.save(inv);
            } catch (Exception e) {
                log.warn("Inventory {} failed for SKU {}: {}", action, item.getSku(), e.getMessage());
            }
        }
    }

    private void reserve(CommerceInventory inv, int qty, String sku) {
        if (inv.getAvailableStock() < qty) {
            throw new IllegalStateException("Insufficient stock for SKU " + sku + ". Available: " + inv.getAvailableStock());
        }
        inv.setAvailableStock(inv.getAvailableStock() - qty);
        inv.setReservedStock(inv.getReservedStock() + qty);
    }

    private void commit(CommerceInventory inv, int qty, String sku) {
        int fromReserved = Math.min(qty, inv.getReservedStock());
        int fromAvailable = qty - fromReserved;
        if (fromAvailable > inv.getAvailableStock()) {
            log.warn("Committing more than available for SKU {}; proceeding anyway.", sku);
            fromAvailable = inv.getAvailableStock();
        }
        inv.setReservedStock(inv.getReservedStock() - fromReserved);
        inv.setAvailableStock(inv.getAvailableStock() - fromAvailable);
        inv.setCommittedStock(inv.getCommittedStock() + qty);
    }

    private void release(CommerceInventory inv, int qty, String sku) {
        int fromCommitted = Math.min(qty, inv.getCommittedStock());
        int fromReserved  = qty - fromCommitted;
        if (fromReserved > inv.getReservedStock()) fromReserved = inv.getReservedStock();
        inv.setCommittedStock(inv.getCommittedStock() - fromCommitted);
        inv.setReservedStock(inv.getReservedStock() - fromReserved);
        inv.setAvailableStock(inv.getAvailableStock() + fromCommitted + fromReserved);
    }

}
