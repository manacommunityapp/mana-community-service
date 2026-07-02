package com.manacommunity.api.repository;

import com.manacommunity.api.model.InventoryItem;
import com.manacommunity.api.model.ItemStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.id = :id")
    Optional<InventoryItem> findByIdForUpdate(@Param("id") Long id);

    Optional<InventoryItem> findByQrCodeId(String qrCodeId);

    long countByStatus(ItemStatus status);
}
