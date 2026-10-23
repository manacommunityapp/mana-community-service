package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.MealPlanResponse;
import com.manacommunity.api.dto.MealSubscriptionRequest;
import com.manacommunity.api.dto.MealSubscriptionResponse;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.MealPlan;
import com.manacommunity.api.model.MealSubscription;
import com.manacommunity.api.model.MealSubscription.SubscriptionStatus;
import com.manacommunity.api.repository.MealPlanRepository;
import com.manacommunity.api.repository.MealSubscriptionRepository;
import com.manacommunity.api.service.MealSubscriptionService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealSubscriptionServiceImpl implements MealSubscriptionService {

    private final MealSubscriptionRepository subscriptionRepository;
    private final MealPlanRepository mealPlanRepository;

    @Override
    public List<MealSubscriptionResponse> getUserSubscriptions(Long userId) {
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MealSubscriptionResponse createSubscription(MealSubscriptionRequest request, AppUser user) {
        MealPlan plan = mealPlanRepository.findById(request.getMealPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("MealPlan", "id", request.getMealPlanId().toString()));

        MealSubscription subscription = MealSubscription.builder()
                .user(user)
                .mealPlan(plan)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(SubscriptionStatus.ACTIVE)
                .deliveryAddress(request.getDeliveryAddress())
                .specialInstructions(request.getSpecialInstructions())
                .build();

        return toResponse(subscriptionRepository.save(subscription));
    }

    @Override
    @Transactional
    public MealSubscriptionResponse pauseSubscription(Long id, Long userId) {
        MealSubscription subscription = subscriptionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("MealSubscription", "id", id.toString()));

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Only active subscriptions can be paused.");
        }
        subscription.setStatus(SubscriptionStatus.PAUSED);
        return toResponse(subscriptionRepository.save(subscription));
    }

    @Override
    @Transactional
    public MealSubscriptionResponse resumeSubscription(Long id, Long userId) {
        MealSubscription subscription = subscriptionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("MealSubscription", "id", id.toString()));

        if (subscription.getStatus() != SubscriptionStatus.PAUSED) {
            throw new IllegalStateException("Only paused subscriptions can be resumed.");
        }
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        return toResponse(subscriptionRepository.save(subscription));
    }

    @Override
    @Transactional
    public void cancelSubscription(Long id, Long userId) {
        MealSubscription subscription = subscriptionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("MealSubscription", "id", id.toString()));

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);
    }

    private MealSubscriptionResponse toResponse(MealSubscription sub) {
        MealPlan plan = sub.getMealPlan();
        return MealSubscriptionResponse.builder()
                .id(sub.getId())
                .userId(sub.getUser().getId())
                .userName(sub.getUser().getFullName())
                .mealPlan(MealPlanResponse.builder()
                        .id(plan.getId())
                        .name(plan.getName())
                        .description(plan.getDescription())
                        .pricePerMeal(plan.getPricePerMeal())
                        .mealsPerDay(plan.getMealsPerDay())
                        .mealType(plan.getMealType().name())
                        .active(plan.getActive())
                        .createdAt(plan.getCreatedAt())
                        .build())
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .status(sub.getStatus().name())
                .deliveryAddress(sub.getDeliveryAddress())
                .specialInstructions(sub.getSpecialInstructions())
                .createdAt(sub.getCreatedAt())
                .build();
    }
}
