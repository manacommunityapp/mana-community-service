package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodGroceryCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodGroceryCategoryRepository extends JpaRepository<FoodGroceryCategory, Long> {

    List<FoodGroceryCategory> findByStoreIdAndActiveOrderBySortOrder(Long storeId, Boolean active);

    List<FoodGroceryCategory> findByStoreIdIsNullAndActiveOrderBySortOrder(Boolean active);
}
