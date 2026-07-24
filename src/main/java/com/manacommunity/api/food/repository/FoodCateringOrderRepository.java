package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodCateringOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodCateringOrderRepository extends JpaRepository<FoodCateringOrder, Long> {

    Optional<FoodCateringOrder> findByRequestId(Long requestId);

    Optional<FoodCateringOrder> findByIdAndCommunityId(Long id, Long communityId);
}
