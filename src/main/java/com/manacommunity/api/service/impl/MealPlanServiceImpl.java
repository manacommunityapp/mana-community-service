package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.MealPlanResponse;
import com.manacommunity.api.model.MealPlan;
import com.manacommunity.api.repository.MealPlanRepository;
import com.manacommunity.api.service.MealPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealPlanServiceImpl implements MealPlanService {

    private final MealPlanRepository mealPlanRepository;

    @Override
    public List<MealPlanResponse> getActiveMealPlans(Long communityId) {
        return mealPlanRepository.findByCommunityIdAndActiveTrueOrderByNameAsc(communityId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private MealPlanResponse toResponse(MealPlan plan) {
        return MealPlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .pricePerMeal(plan.getPricePerMeal())
                .mealsPerDay(plan.getMealsPerDay())
                .mealType(plan.getMealType().name())
                .active(plan.getActive())
                .createdAt(plan.getCreatedAt())
                .build();
    }
}
