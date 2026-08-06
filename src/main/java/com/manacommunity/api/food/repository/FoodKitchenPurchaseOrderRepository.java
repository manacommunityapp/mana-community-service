package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodKitchenPurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodKitchenPurchaseOrderRepository extends JpaRepository<FoodKitchenPurchaseOrder, Long> {

    Page<FoodKitchenPurchaseOrder> findByKitchenTypeAndKitchenId(String kitchenType, Long kitchenId, Pageable pageable);

    Optional<FoodKitchenPurchaseOrder> findByIdAndCommunityId(Long id, Long communityId);
}
