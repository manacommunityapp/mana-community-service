package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodWeightLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FoodWeightLogRepository extends JpaRepository<FoodWeightLog, Long> {

    List<FoodWeightLog> findByUserIdOrderByDateDesc(Long userId);

    List<FoodWeightLog> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
}
