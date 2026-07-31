package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodAnalyticsDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FoodAnalyticsDailyRepository extends JpaRepository<FoodAnalyticsDaily, Long> {

    List<FoodAnalyticsDaily> findByEntityTypeAndEntityIdAndDateBetween(String entityType, Long entityId, LocalDate startDate, LocalDate endDate);
}
