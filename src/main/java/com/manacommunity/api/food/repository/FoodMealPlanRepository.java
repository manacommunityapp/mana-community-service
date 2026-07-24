package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodMealPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodMealPlanRepository extends JpaRepository<FoodMealPlan, Long> {

    List<FoodMealPlan> findByUserIdAndCommunityId(Long userId, Long communityId);

    List<FoodMealPlan> findByUserIdAndStatus(Long userId, String status);
}
