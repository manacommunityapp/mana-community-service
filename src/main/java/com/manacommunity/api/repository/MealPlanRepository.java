package com.manacommunity.api.repository;

import com.manacommunity.api.model.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {

    List<MealPlan> findByCommunityIdAndActiveTrueOrderByNameAsc(Long communityId);

    List<MealPlan> findByCommunityIdOrderByNameAsc(Long communityId);
}
