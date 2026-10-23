package com.manacommunity.api.repository;

import com.manacommunity.api.model.GroceryOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroceryOrderRepository extends JpaRepository<GroceryOrder, Long> {

    List<GroceryOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<GroceryOrder> findByIdAndUserId(Long id, Long userId);
}
