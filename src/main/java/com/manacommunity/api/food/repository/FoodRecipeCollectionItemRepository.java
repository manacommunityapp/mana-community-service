package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodRecipeCollectionItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodRecipeCollectionItemRepository extends JpaRepository<FoodRecipeCollectionItem, Long> {
}
