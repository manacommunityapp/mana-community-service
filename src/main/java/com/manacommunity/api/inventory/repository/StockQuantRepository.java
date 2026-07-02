package com.manacommunity.api.inventory.repository;

import com.manacommunity.api.inventory.entity.StockQuant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockQuantRepository extends JpaRepository<StockQuant, Long> {
    List<StockQuant> findByProductId(Long productId);
    List<StockQuant> findByLocationId(Long locationId);

    @Query("SELECT q FROM StockQuant q WHERE q.productId = :productId AND q.locationId = :locationId AND " +
           "((q.lotId IS NULL AND :lotId IS NULL) OR (q.lotId = :lotId))")
    Optional<StockQuant> findByProductLocationAndLot(@Param("productId") Long productId, 
                                                    @Param("locationId") Long locationId, 
                                                    @Param("lotId") Long lotId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM StockQuant q WHERE q.productId = :productId AND q.locationId = :locationId AND " +
           "((q.lotId IS NULL AND :lotId IS NULL) OR (q.lotId = :lotId))")
    Optional<StockQuant> findForUpdate(@Param("productId") Long productId, 
                                       @Param("locationId") Long locationId, 
                                       @Param("lotId") Long lotId);
}
