package com.manacommunity.api.repository;

import com.manacommunity.api.model.InventoryStockQuant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryStockQuantRepository extends JpaRepository<InventoryStockQuant, Long> {

    Optional<InventoryStockQuant> findByProductIdAndLocationId(Long productId, Long locationId);

    Optional<InventoryStockQuant> findByProductIdAndLocationIdAndLotId(Long productId, Long locationId, Long lotId);

    List<InventoryStockQuant> findByProductId(Long productId);

    List<InventoryStockQuant> findByLocationId(Long locationId);

    /** Total on-hand across all INTERNAL locations for a product. */
    @Query("SELECT COALESCE(SUM(q.quantity), 0) FROM InventoryStockQuant q " +
           "WHERE q.product.id = :productId AND q.location.usage = 'INTERNAL'")
    BigDecimal sumOnHandByProduct(@Param("productId") Long productId);

    /** Total reserved across all INTERNAL locations for a product. */
    @Query("SELECT COALESCE(SUM(q.reservedQty), 0) FROM InventoryStockQuant q " +
           "WHERE q.product.id = :productId AND q.location.usage = 'INTERNAL'")
    BigDecimal sumReservedByProduct(@Param("productId") Long productId);

    /** Stock levels report — one row per product per location with on-hand > 0. */
    @Query("SELECT q FROM InventoryStockQuant q " +
           "JOIN FETCH q.product JOIN FETCH q.location " +
           "WHERE q.quantity <> 0 ORDER BY q.product.name, q.location.completeName")
    List<InventoryStockQuant> findAllNonZero();
}
