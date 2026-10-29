package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodGroceryOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodGroceryOrderRepository extends JpaRepository<FoodGroceryOrder, Long> {

    Page<FoodGroceryOrder> findByUserIdAndCommunityId(Long userId, Long communityId, Pageable pageable);

    Page<FoodGroceryOrder> findByStoreIdAndCommunityId(Long storeId, Long communityId, Pageable pageable);

    @Query("SELECT o FROM FoodGroceryOrder o WHERE o.store.owner.id = :ownerId AND o.community.id = :communityId")
    Page<FoodGroceryOrder> findVendorOrders(@Param("ownerId") Long ownerId,
            @Param("communityId") Long communityId, Pageable pageable);

    @Query("SELECT o FROM FoodGroceryOrder o WHERE o.id = :orderId AND o.store.owner.id = :ownerId AND o.community.id = :communityId")
    Optional<FoodGroceryOrder> findVendorOrder(@Param("orderId") Long orderId,
            @Param("ownerId") Long ownerId, @Param("communityId") Long communityId);

    Optional<FoodGroceryOrder> findByIdAndCommunityId(Long id, Long communityId);

    Optional<FoodGroceryOrder> findByOrderNumber(String orderNumber);
}
