package com.manacommunity.api.repository;

import com.manacommunity.api.model.InventoryProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryProductRepository extends JpaRepository<InventoryProduct, Long> {

    List<InventoryProduct> findByIsActiveTrueOrderByNameAsc();

    List<InventoryProduct> findByCommunityIdAndIsActiveTrueOrderByNameAsc(Long communityId);

    Page<InventoryProduct> findByIsActiveTrue(Pageable pageable);

    Page<InventoryProduct> findByCommunityIdAndIsActiveTrue(Long communityId, Pageable pageable);

    List<InventoryProduct> findByCategoryIdAndIsActiveTrueOrderByNameAsc(Long categoryId);

    Optional<InventoryProduct> findByDefaultCode(String defaultCode);

    Optional<InventoryProduct> findByBarcode(String barcode);

    @Query("SELECT p FROM InventoryProduct p WHERE p.isActive = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(p.defaultCode) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<InventoryProduct> search(@Param("q") String query);
}
