package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodCommunityKitchenMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FoodCommunityKitchenMenuRepository extends JpaRepository<FoodCommunityKitchenMenu, Long> {

    List<FoodCommunityKitchenMenu> findByKitchenIdAndDate(Long kitchenId, LocalDate date);

    List<FoodCommunityKitchenMenu> findByKitchenIdAndDateAndMealType(Long kitchenId, LocalDate date, FoodCommunityKitchenMenu.MealType mealType);
}
