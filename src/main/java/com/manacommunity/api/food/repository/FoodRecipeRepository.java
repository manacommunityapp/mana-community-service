package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodRecipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FoodRecipeRepository extends JpaRepository<FoodRecipe, Long> {

    Page<FoodRecipe> findByCommunityIdAndStatus(Long communityId, String status, Pageable pageable);

    Page<FoodRecipe> findByAuthorId(Long authorId, Pageable pageable);

    Optional<FoodRecipe> findByIdAndCommunityId(Long id, Long communityId);

    @Query("SELECT r FROM FoodRecipe r WHERE r.community.id = :communityId AND r.status = 'PUBLISHED' " +
           "AND (LOWER(r.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<FoodRecipe> searchByCommunity(@Param("communityId") Long communityId, @Param("query") String query, Pageable pageable);
}
