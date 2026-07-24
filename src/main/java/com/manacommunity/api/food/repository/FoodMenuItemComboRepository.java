package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodMenuItemCombo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodMenuItemComboRepository extends JpaRepository<FoodMenuItemCombo, Long> {

    List<FoodMenuItemCombo> findByRestaurantIdAndActive(Long restaurantId, Boolean active);
}
