package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodGroceryProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FoodGroceryProductRepository extends JpaRepository<FoodGroceryProduct, Long> {

    Page<FoodGroceryProduct> findByStoreIdAndActive(Long storeId, Boolean active, Pageable pageable);

    Page<FoodGroceryProduct> findByCategoryIdAndActive(Long categoryId, Boolean active, Pageable pageable);

    Page<FoodGroceryProduct> findByStoreIdAndIsFeatured(Long storeId, Boolean isFeatured, Pageable pageable);

    @Query("SELECT p FROM FoodGroceryProduct p WHERE p.store.id = :storeId " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<FoodGroceryProduct> searchByStore(@Param("storeId") Long storeId, @Param("query") String query, Pageable pageable);
}
