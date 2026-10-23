package com.manacommunity.api.repository;

import com.manacommunity.api.model.ProcurementCategory;
import com.manacommunity.api.model.SupplierProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierProductRepository extends JpaRepository<SupplierProduct, Long> {

    List<SupplierProduct> findBySupplierIdAndActiveTrue(Long supplierId);

    List<SupplierProduct> findBySupplierIdAndCategoryAndActiveTrue(Long supplierId, ProcurementCategory category);

    List<SupplierProduct> findByCategoryAndActiveTrue(ProcurementCategory category);

    @Query("SELECT p FROM SupplierProduct p WHERE p.active = true " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<SupplierProduct> search(@Param("category") ProcurementCategory category, @Param("search") String search);
}
