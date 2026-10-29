package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodGroceryOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodGroceryOrderItemRepository extends JpaRepository<FoodGroceryOrderItem, Long> {
    List<FoodGroceryOrderItem> findByOrderId(Long orderId);
}
