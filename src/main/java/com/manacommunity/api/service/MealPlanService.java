package com.manacommunity.api.service;

import com.manacommunity.api.dto.MealPlanResponse;

import java.util.List;

public interface MealPlanService {

    List<MealPlanResponse> getActiveMealPlans(Long communityId);
}
