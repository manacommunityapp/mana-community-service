package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodKitchenSupplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodKitchenSupplierRepository extends JpaRepository<FoodKitchenSupplier, Long> {

    List<FoodKitchenSupplier> findByCommunityIdAndActive(Long communityId, Boolean active);
}
