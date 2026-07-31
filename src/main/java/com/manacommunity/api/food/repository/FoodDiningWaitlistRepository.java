package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodDiningWaitlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodDiningWaitlistRepository extends JpaRepository<FoodDiningWaitlist, Long> {

    List<FoodDiningWaitlist> findByRestaurantIdAndStatus(Long restaurantId, FoodDiningWaitlist.WaitlistStatus status);
}
