package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodCommunityKitchen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodCommunityKitchenRepository extends JpaRepository<FoodCommunityKitchen, Long> {

    List<FoodCommunityKitchen> findByCommunityId(Long communityId);

    Optional<FoodCommunityKitchen> findByIdAndCommunityId(Long id, Long communityId);

    List<FoodCommunityKitchen> findByCommunityIdAndStatus(Long communityId, FoodCommunityKitchen.CommunityKitchenStatus status);
}
