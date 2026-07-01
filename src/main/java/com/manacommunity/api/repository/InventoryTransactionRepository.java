package com.manacommunity.api.repository;

import com.manacommunity.api.model.InventoryItem;
import com.manacommunity.api.model.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    Optional<InventoryTransaction> findFirstByItemAndReturnedAtIsNull(InventoryItem item);
}
