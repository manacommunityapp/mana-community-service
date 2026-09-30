package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodCloudKitchenSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodCloudKitchenSlotRepository extends JpaRepository<FoodCloudKitchenSlot, Long> {

    List<FoodCloudKitchenSlot> findByKitchenId(Long kitchenId);

    List<FoodCloudKitchenSlot> findByKitchenIdAndStatus(Long kitchenId, FoodCloudKitchenSlot.SlotStatus status);
}
