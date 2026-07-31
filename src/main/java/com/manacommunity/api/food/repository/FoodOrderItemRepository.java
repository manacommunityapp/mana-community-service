package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodOrderItemRepository extends JpaRepository<FoodOrderItem, Long> {

    List<FoodOrderItem> findByOrderId(Long orderId);
}
