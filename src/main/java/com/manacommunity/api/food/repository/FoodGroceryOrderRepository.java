package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodGroceryOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodGroceryOrderRepository extends JpaRepository<FoodGroceryOrder, Long> {

    Page<FoodGroceryOrder> findByUserIdAndCommunityId(Long userId, Long communityId, Pageable pageable);

    Page<FoodGroceryOrder> findByStoreIdAndCommunityId(Long storeId, Long communityId, Pageable pageable);

    Optional<FoodGroceryOrder> findByIdAndCommunityId(Long id, Long communityId);

    Optional<FoodGroceryOrder> findByOrderNumber(String orderNumber);
}
