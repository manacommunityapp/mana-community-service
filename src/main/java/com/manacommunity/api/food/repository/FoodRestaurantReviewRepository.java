package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodRestaurantReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodRestaurantReviewRepository extends JpaRepository<FoodRestaurantReview, Long> {

    Page<FoodRestaurantReview> findByRestaurantId(Long restaurantId, Pageable pageable);

    Optional<FoodRestaurantReview> findByRestaurantIdAndUserId(Long restaurantId, Long userId);
}
