package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodCloudKitchen;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodCloudKitchenRepository extends JpaRepository<FoodCloudKitchen, Long> {

    Page<FoodCloudKitchen> findByCommunityId(Long communityId, Pageable pageable);

    Optional<FoodCloudKitchen> findByIdAndCommunityId(Long id, Long communityId);
}
