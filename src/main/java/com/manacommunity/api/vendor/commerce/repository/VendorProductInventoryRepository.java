package com.manacommunity.api.vendor.commerce.repository;

import com.manacommunity.api.vendor.commerce.model.VendorProductInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorProductInventoryRepository extends JpaRepository<VendorProductInventory, Long> {
    
    Optional<VendorProductInventory> findByVariantId(Long variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM VendorProductInventory i WHERE i.variant.id = :variantId")
    Optional<VendorProductInventory> findByVariantIdForUpdate(@Param("variantId") Long variantId);
}
