package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodRecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodRecipeIngredientRepository extends JpaRepository<FoodRecipeIngredient, Long> {

    List<FoodRecipeIngredient> findByRecipeId(Long recipeId);
}
