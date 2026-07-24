package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodSubscriptionPause;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodSubscriptionPauseRepository extends JpaRepository<FoodSubscriptionPause, Long> {

    List<FoodSubscriptionPause> findBySubscriptionId(Long subscriptionId);
}
