package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodCloudKitchenAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FoodCloudKitchenAnalyticsRepository extends JpaRepository<FoodCloudKitchenAnalytics, Long> {

    List<FoodCloudKitchenAnalytics> findByKitchenIdAndDateBetween(Long kitchenId, LocalDate startDate, LocalDate endDate);

    List<FoodCloudKitchenAnalytics> findByKitchenId(Long kitchenId);
}
