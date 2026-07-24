package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodRestaurantAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FoodRestaurantAnalyticsRepository extends JpaRepository<FoodRestaurantAnalytics, Long> {

    List<FoodRestaurantAnalytics> findByRestaurantIdAndDateBetween(Long restaurantId, LocalDate startDate, LocalDate endDate);
}
