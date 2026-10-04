package com.manacommunity.api.service;

import com.manacommunity.api.dto.MealSubscriptionRequest;
import com.manacommunity.api.dto.MealSubscriptionResponse;
import com.manacommunity.api.user.model.AppUser;

import java.util.List;

public interface MealSubscriptionService {

    List<MealSubscriptionResponse> getUserSubscriptions(Long userId);

    MealSubscriptionResponse createSubscription(MealSubscriptionRequest request, AppUser user);

    MealSubscriptionResponse pauseSubscription(Long id, Long userId);

    MealSubscriptionResponse resumeSubscription(Long id, Long userId);

    void cancelSubscription(Long id, Long userId);
}
