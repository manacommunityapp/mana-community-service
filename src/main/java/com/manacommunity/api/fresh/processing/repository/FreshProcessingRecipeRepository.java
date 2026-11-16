package com.manacommunity.api.fresh.processing.repository;

import com.manacommunity.api.fresh.processing.model.FreshProcessingRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FreshProcessingRecipeRepository extends JpaRepository<FreshProcessingRecipe, Long> {
    List<FreshProcessingRecipe> findByVendorId(Long vendorId);
    Optional<FreshProcessingRecipe> findByRecipeCode(String recipeCode);
}