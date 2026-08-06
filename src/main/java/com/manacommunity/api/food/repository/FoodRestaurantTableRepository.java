package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodRestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodRestaurantTableRepository extends JpaRepository<FoodRestaurantTable, Long> {

    List<FoodRestaurantTable> findByRestaurantIdAndStatus(Long restaurantId, FoodRestaurantTable.TableStatus status);

    List<FoodRestaurantTable> findByRestaurantId(Long restaurantId);
}
