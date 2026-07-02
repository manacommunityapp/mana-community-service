package com.manacommunity.api.service;

import com.manacommunity.api.dto.CheckoutRequest;
import com.manacommunity.api.model.InventoryItem;
import com.manacommunity.api.model.InventoryTransaction;
import com.manacommunity.api.model.ItemStatus;
import com.manacommunity.api.repository.InventoryItemRepository;
import com.manacommunity.api.repository.InventoryTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryService {

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    public List<InventoryItem> getAllItems() {
        List<InventoryItem> items = inventoryItemRepository.findAll();
        for (InventoryItem item : items) {
            populateCheckoutDetails(item);
        }
        return items;
    }

    public Optional<InventoryItem> getItemById(Long id) {
        return inventoryItemRepository.findById(id).map(item -> {
            populateCheckoutDetails(item);
            return item;
        });
    }

    public Optional<InventoryItem> getItemByQrCode(String qrCodeId) {
        return inventoryItemRepository.findByQrCodeId(qrCodeId).map(item -> {
            populateCheckoutDetails(item);
            return item;
        });
    }

    public InventoryItem createItem(InventoryItem item) {
        if (item.getStatus() == null) {
            item.setStatus(ItemStatus.AVAILABLE);
        }
        if (item.getQrCodeId() == null || item.getQrCodeId().isBlank()) {
            item.setQrCodeId(UUID.randomUUID().toString().substring(0, 8));
        }
        return inventoryItemRepository.save(item);
    }

    private void populateCheckoutDetails(InventoryItem item) {
        if (item.getStatus() == ItemStatus.BORROWED) {
            inventoryTransactionRepository.findFirstByItemAndReturnedAtIsNull(item).ifPresent(txn -> {
                item.setBorrowedBy(txn.getBorrowedBy());
                item.setBorrowedByFlat(txn.getBorrowedByFlat());
                item.setBorrowedAt(txn.getBorrowedAt().toString());
                item.setExpectedReturn(txn.getExpectedReturnAt());
            });
        }
    }

    @Transactional
    public Optional<InventoryItem> checkoutItem(Long id, CheckoutRequest request) {
        // Enforce pessimistic lock (concurrency select for update)
        Optional<InventoryItem> itemOpt = inventoryItemRepository.findByIdForUpdate(id);
        
        if (itemOpt.isPresent()) {
            InventoryItem item = itemOpt.get();
            if (item.getStatus() == ItemStatus.AVAILABLE) {
                // Mutate status to BORROWED
                item.setStatus(ItemStatus.BORROWED);
                InventoryItem updatedItem = inventoryItemRepository.save(item);

                // Create log transaction
                InventoryTransaction txn = InventoryTransaction.builder()
                        .item(updatedItem)
                        .borrowedBy(request.getBorrowedBy())
                        .borrowedByFlat(request.getBorrowedByFlat())
                        .borrowedAt(LocalDateTime.now())
                        .expectedReturnAt(request.getExpectedReturnAt())
                        .build();
                inventoryTransactionRepository.save(txn);

                return Optional.of(updatedItem);
            }
        }
        return Optional.empty();
    }

    @Transactional
    public Optional<InventoryItem> checkinItem(Long id) {
        Optional<InventoryItem> itemOpt = inventoryItemRepository.findByIdForUpdate(id);
        
        if (itemOpt.isPresent()) {
            InventoryItem item = itemOpt.get();
            if (item.getStatus() == ItemStatus.BORROWED) {
                // Find active transaction
                Optional<InventoryTransaction> txnOpt = inventoryTransactionRepository
                        .findFirstByItemAndReturnedAtIsNull(item);
                
                if (txnOpt.isPresent()) {
                    InventoryTransaction txn = txnOpt.get();
                    txn.setReturnedAt(LocalDateTime.now());
                    inventoryTransactionRepository.save(txn);
                }

                // Restore item status
                item.setStatus(ItemStatus.AVAILABLE);
                return Optional.of(inventoryItemRepository.save(item));
            }
        }
        return Optional.empty();
    }

    @Transactional
    public Optional<InventoryItem> updateStatus(Long id, String status) {
        return inventoryItemRepository.findByIdForUpdate(id).map(item -> {
            item.setStatus(ItemStatus.valueOf(status));
            return inventoryItemRepository.save(item);
        });
    }

    @Transactional
    public Optional<InventoryItem> recordMaintenance(Long id, String nextDueDate) {
        return inventoryItemRepository.findByIdForUpdate(id).map(item -> {
            item.setStatus(ItemStatus.MAINTENANCE);
            if (nextDueDate != null && !nextDueDate.isBlank()) {
                item.setNextMaintenanceDueAt(LocalDate.parse(nextDueDate));
            }
            return inventoryItemRepository.save(item);
        });
    }

    @Transactional
    public Optional<InventoryItem> recordAudit(Long id, int expectedQuantity, int actualQuantity) {
        return inventoryItemRepository.findByIdForUpdate(id).map(item -> {
            item.setLastAuditedAt(LocalDateTime.now());
            item.setAuditVariance(actualQuantity - expectedQuantity);
            if (actualQuantity <= 0) {
                item.setStatus(ItemStatus.LOST);
            }
            return inventoryItemRepository.save(item);
        });
    }
}
