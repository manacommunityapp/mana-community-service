package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodRecipeCollection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodRecipeCollectionRepository extends JpaRepository<FoodRecipeCollection, Long> {

    List<FoodRecipeCollection> findByUserIdAndCommunityId(Long userId, Long communityId);

    Page<FoodRecipeCollection> findByCommunityIdAndIsPublic(Long communityId, Boolean isPublic, Pageable pageable);
}
