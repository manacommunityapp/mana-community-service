package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodEventFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodEventFeedbackRepository extends JpaRepository<FoodEventFeedback, Long> {

    List<FoodEventFeedback> findByEventId(Long eventId);
}
