package com.manacommunity.api.repository;

import com.manacommunity.api.model.StockLedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockLedgerEntryRepository extends JpaRepository<StockLedgerEntry, Long> {

    List<StockLedgerEntry> findByProductIdOrderByCreatedAtDesc(Long productId);

    List<StockLedgerEntry> findByProductIdAndLocationIdOrderByCreatedAtDesc(Long productId, Long locationId);

    Page<StockLedgerEntry> findByCommunityIdOrderByCreatedAtDesc(Long communityId, Pageable pageable);

    @Query("SELECT e FROM StockLedgerEntry e JOIN FETCH e.product JOIN FETCH e.location " +
           "WHERE (:communityId IS NULL OR e.community.id = :communityId) " +
           "AND (:productId IS NULL OR e.product.id = :productId) " +
           "ORDER BY e.createdAt DESC")
    Page<StockLedgerEntry> searchLedger(
            @Param("communityId") Long communityId,
            @Param("productId") Long productId,
            Pageable pageable);
}
