package com.manacommunity.api.wellness.repository;

import com.manacommunity.api.wellness.entity.MealLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MealLogRepository extends JpaRepository<MealLog, Long> {
    List<MealLog> findByUserIdAndMealDateOrderByMealTimeDesc(Long userId, LocalDate mealDate);
    List<MealLog> findByUserIdAndMealDateBetweenOrderByMealDateDescMealTimeDesc(Long userId, LocalDate start, LocalDate end);
}
