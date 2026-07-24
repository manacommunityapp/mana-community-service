package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodSubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodSubscriptionPlanRepository extends JpaRepository<FoodSubscriptionPlan, Long> {

    List<FoodSubscriptionPlan> findByCommunityIdAndActive(Long communityId, Boolean active);

    Optional<FoodSubscriptionPlan> findByIdAndCommunityId(Long id, Long communityId);

    List<FoodSubscriptionPlan> findByCommunityIdAndTargetAudience(Long communityId, FoodSubscriptionPlan.TargetAudience targetAudience);
}
