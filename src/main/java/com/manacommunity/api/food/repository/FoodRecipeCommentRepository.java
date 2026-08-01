package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodRecipeComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodRecipeCommentRepository extends JpaRepository<FoodRecipeComment, Long> {

    Page<FoodRecipeComment> findByRecipeId(Long recipeId, Pageable pageable);
}
