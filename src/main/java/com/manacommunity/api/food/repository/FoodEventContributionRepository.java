package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodEventContribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodEventContributionRepository extends JpaRepository<FoodEventContribution, Long> {

    List<FoodEventContribution> findByEventId(Long eventId);
}
