package com.manacommunity.api.repository;

import com.manacommunity.api.model.GroceryOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroceryOrderItemRepository extends JpaRepository<GroceryOrderItem, Long> {
}
