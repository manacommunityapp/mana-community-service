package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodMenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodMenuCategoryRepository extends JpaRepository<FoodMenuCategory, Long> {

    List<FoodMenuCategory> findByRestaurantIdAndActiveOrderBySortOrder(Long restaurantId, Boolean active);

    List<FoodMenuCategory> findByRestaurantId(Long restaurantId);
}
