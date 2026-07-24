package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {

    Page<FoodOrder> findByUserIdAndCommunityId(Long userId, Long communityId, Pageable pageable);

    Page<FoodOrder> findByProviderTypeAndProviderIdAndCommunityId(FoodOrder.ProviderType providerType, Long providerId, Long communityId, Pageable pageable);

    Optional<FoodOrder> findByIdAndCommunityId(Long id, Long communityId);

    Optional<FoodOrder> findByOrderNumber(String orderNumber);

    Page<FoodOrder> findByStatusAndCommunityId(FoodOrder.OrderStatus status, Long communityId, Pageable pageable);

    long countByProviderTypeAndProviderIdAndStatus(FoodOrder.ProviderType providerType, Long providerId, FoodOrder.OrderStatus status);
}
