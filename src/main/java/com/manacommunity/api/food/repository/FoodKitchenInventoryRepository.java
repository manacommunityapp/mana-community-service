package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodKitchenInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FoodKitchenInventoryRepository extends JpaRepository<FoodKitchenInventory, Long> {

    List<FoodKitchenInventory> findByKitchenTypeAndKitchenId(String kitchenType, Long kitchenId);

    @Query("SELECT i FROM FoodKitchenInventory i WHERE i.kitchenType = :kitchenType AND i.kitchenId = :kitchenId " +
           "AND i.currentStock < i.reorderLevel")
    List<FoodKitchenInventory> findByKitchenTypeAndKitchenIdAndCurrentStockLessThanReorderLevel(
            @Param("kitchenType") String kitchenType, @Param("kitchenId") Long kitchenId);
}
