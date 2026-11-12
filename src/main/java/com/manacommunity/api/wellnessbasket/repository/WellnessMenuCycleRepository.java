package com.manacommunity.api.wellnessbasket.repository;

import com.manacommunity.api.wellnessbasket.entity.WellnessMenuCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WellnessMenuCycleRepository extends JpaRepository<WellnessMenuCycle, Long> {

    Optional<WellnessMenuCycle> findByPlanTypeAndCycleWeekAndDayOfWeek(String planType, Short cycleWeek, Short dayOfWeek);

    List<WellnessMenuCycle> findByPlanTypeAndCycleWeek(String planType, Short cycleWeek);

    List<WellnessMenuCycle> findByPlanType(String planType);
}
