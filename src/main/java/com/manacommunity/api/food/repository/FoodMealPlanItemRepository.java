package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodMealPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodMealPlanItemRepository extends JpaRepository<FoodMealPlanItem, Long> {

    List<FoodMealPlanItem> findByPlanId(Long planId);

    List<FoodMealPlanItem> findByPlanIdAndDayOfWeek(Long planId, String dayOfWeek);
}
