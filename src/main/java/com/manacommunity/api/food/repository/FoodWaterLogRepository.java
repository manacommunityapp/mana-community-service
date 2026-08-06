package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodWaterLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FoodWaterLogRepository extends JpaRepository<FoodWaterLog, Long> {

    List<FoodWaterLog> findByUserIdAndDate(Long userId, LocalDate date);

    List<FoodWaterLog> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
}
