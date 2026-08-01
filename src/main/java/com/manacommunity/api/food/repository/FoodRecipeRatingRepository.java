package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodRecipeRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodRecipeRatingRepository extends JpaRepository<FoodRecipeRating, Long> {

    Page<FoodRecipeRating> findByRecipeId(Long recipeId, Pageable pageable);

    Optional<FoodRecipeRating> findByRecipeIdAndUserId(Long recipeId, Long userId);
}
