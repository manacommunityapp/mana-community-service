package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodCalorieLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FoodCalorieLogRepository extends JpaRepository<FoodCalorieLog, Long> {

    List<FoodCalorieLog> findByUserIdAndDate(Long userId, LocalDate date);

    List<FoodCalorieLog> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
}
