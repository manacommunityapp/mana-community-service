package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodPantryShoppingListItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodPantryShoppingListItemRepository extends JpaRepository<FoodPantryShoppingListItem, Long> {

    List<FoodPantryShoppingListItem> findByListId(Long listId);

    List<FoodPantryShoppingListItem> findByListIdAndIsPurchased(Long listId, Boolean isPurchased);
}
