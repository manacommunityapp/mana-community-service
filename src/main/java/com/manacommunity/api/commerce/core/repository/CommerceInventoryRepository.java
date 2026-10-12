package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommerceInventoryRepository extends JpaRepository<CommerceInventory, Long> {

    Optional<CommerceInventory> findByProduct_Id(Long productId);

    Optional<CommerceInventory> findByProduct_Sku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM CommerceInventory i WHERE i.product.sku = :sku")
    Optional<CommerceInventory> findBySkuForUpdate(@Param("sku") String sku);
}
