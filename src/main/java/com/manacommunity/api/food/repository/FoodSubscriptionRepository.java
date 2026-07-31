package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodSubscriptionRepository extends JpaRepository<FoodSubscription, Long> {

    List<FoodSubscription> findByUserIdAndCommunityId(Long userId, Long communityId);

    List<FoodSubscription> findByUserIdAndStatus(Long userId, FoodSubscription.SubscriptionStatus status);

    Page<FoodSubscription> findByPlanId(Long planId, Pageable pageable);
}
