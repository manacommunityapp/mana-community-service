package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodOrderRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodOrderRatingRepository extends JpaRepository<FoodOrderRating, Long> {

    Optional<FoodOrderRating> findByOrderId(Long orderId);
}
