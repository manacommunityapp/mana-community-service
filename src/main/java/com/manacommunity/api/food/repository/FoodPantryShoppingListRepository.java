package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodPantryShoppingList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodPantryShoppingListRepository extends JpaRepository<FoodPantryShoppingList, Long> {

    List<FoodPantryShoppingList> findByUserIdAndStatus(Long userId, String status);

    List<FoodPantryShoppingList> findByUserIdAndCommunityId(Long userId, Long communityId);
}
