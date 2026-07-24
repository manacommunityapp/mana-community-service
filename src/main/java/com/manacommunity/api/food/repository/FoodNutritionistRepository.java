package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodNutritionist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodNutritionistRepository extends JpaRepository<FoodNutritionist, Long> {

    List<FoodNutritionist> findByCommunityIdAndStatus(Long communityId, String status);

    Optional<FoodNutritionist> findByIdAndCommunityId(Long id, Long communityId);

    Optional<FoodNutritionist> findByUserIdAndCommunityId(Long userId, Long communityId);
}
