package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodResidentGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodResidentGoalRepository extends JpaRepository<FoodResidentGoal, Long> {

    List<FoodResidentGoal> findByProfileIdAndStatus(Long profileId, FoodResidentGoal.GoalStatus status);
}
