package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodRestaurantOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodRestaurantOfferRepository extends JpaRepository<FoodRestaurantOffer, Long> {

    List<FoodRestaurantOffer> findByRestaurantIdAndActive(Long restaurantId, Boolean active);

    Optional<FoodRestaurantOffer> findByRestaurantIdAndCouponCode(Long restaurantId, String couponCode);
}
