package com.manacommunity.api.food.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.food.entity.*;
import com.manacommunity.api.food.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodSubscriptionService {

    private final FoodSubscriptionPlanRepository planRepo;
    private final FoodSubscriptionRepository subscriptionRepo;
    private final FoodSubscriptionDeliveryRepository deliveryRepo;
    private final FoodSubscriptionPauseRepository pauseRepo;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPlans(Long communityId, String targetAudience) {
        List<FoodSubscriptionPlan> plans;
        if (targetAudience != null && !targetAudience.isBlank()) {
            plans = planRepo.findByCommunityIdAndTargetAudience(communityId,
                    FoodSubscriptionPlan.TargetAudience.valueOf(targetAudience));
        } else {
            plans = planRepo.findByCommunityIdAndActive(communityId, true);
        }
        return plans.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPlanById(Long communityId, Long id) {
        FoodSubscriptionPlan plan = planRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan", id));
        return toResponse(plan);
    }

    @Transactional
    public Map<String, Object> createPlan(Long communityId, Map<String, Object> request) {
        Community community = new Community();
        community.setId(communityId);

        FoodSubscriptionPlan plan = FoodSubscriptionPlan.builder()
                .name((String) request.get("name"))
                .description((String) request.get("description"))
                .planType(request.get("planType") != null ?
                        FoodSubscriptionPlan.PlanType.valueOf((String) request.get("planType")) : null)
                .targetAudience(request.get("targetAudience") != null ?
                        FoodSubscriptionPlan.TargetAudience.valueOf((String) request.get("targetAudience")) : null)
                .providerType(request.get("providerType") != null ?
                        FoodSubscriptionPlan.ProviderType.valueOf((String) request.get("providerType")) : null)
                .providerId(request.get("providerId") != null ?
                        Long.valueOf(request.get("providerId").toString()) : null)
                .pricePerMeal(request.get("pricePerMeal") != null ?
                        new java.math.BigDecimal(request.get("pricePerMeal").toString()) : null)
                .monthlyPrice(request.get("monthlyPrice") != null ?
                        new java.math.BigDecimal(request.get("monthlyPrice").toString()) : null)
                .minDays(request.get("minDays") != null ?
                        Integer.valueOf(request.get("minDays").toString()) : null)
                .includesWeekends(request.get("includesWeekends") != null ?
                        (Boolean) request.get("includesWeekends") : true)
                .imageUrl((String) request.get("imageUrl"))
                .nutritionInfo((String) request.get("nutritionInfo"))
                .community(community)
                .build();

        return toResponse(planRepo.save(plan));
    }

    @Transactional
    public Map<String, Object> updatePlan(Long communityId, Long id, Map<String, Object> request) {
        FoodSubscriptionPlan plan = planRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan", id));

        if (request.containsKey("name")) plan.setName((String) request.get("name"));
        if (request.containsKey("description")) plan.setDescription((String) request.get("description"));
        if (request.containsKey("planType"))
            plan.setPlanType(FoodSubscriptionPlan.PlanType.valueOf((String) request.get("planType")));
        if (request.containsKey("targetAudience"))
            plan.setTargetAudience(FoodSubscriptionPlan.TargetAudience.valueOf((String) request.get("targetAudience")));
        if (request.containsKey("pricePerMeal"))
            plan.setPricePerMeal(new java.math.BigDecimal(request.get("pricePerMeal").toString()));
        if (request.containsKey("monthlyPrice"))
            plan.setMonthlyPrice(new java.math.BigDecimal(request.get("monthlyPrice").toString()));
        if (request.containsKey("minDays"))
            plan.setMinDays(Integer.valueOf(request.get("minDays").toString()));
        if (request.containsKey("includesWeekends"))
            plan.setIncludesWeekends((Boolean) request.get("includesWeekends"));
        if (request.containsKey("active"))
            plan.setActive((Boolean) request.get("active"));
        if (request.containsKey("imageUrl")) plan.setImageUrl((String) request.get("imageUrl"));
        if (request.containsKey("nutritionInfo")) plan.setNutritionInfo((String) request.get("nutritionInfo"));

        return toResponse(planRepo.save(plan));
    }

    @Transactional
    public Map<String, Object> subscribe(Long communityId, Map<String, Object> request, AppUser user) {
        Community community = user.getCommunity();
        Long planId = Long.valueOf(request.get("planId").toString());
        FoodSubscriptionPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan", planId));

        FoodSubscription subscription = FoodSubscription.builder()
                .plan(plan)
                .user(user)
                .startDate(request.get("startDate") != null ?
                        LocalDate.parse((String) request.get("startDate")) : LocalDate.now())
                .endDate(request.get("endDate") != null ?
                        LocalDate.parse((String) request.get("endDate")) : null)
                .status(FoodSubscription.SubscriptionStatus.ACTIVE)
                .autoRenew(request.get("autoRenew") != null ?
                        (Boolean) request.get("autoRenew") : true)
                .deliveryAddress((String) request.get("deliveryAddress"))
                .deliveryInstructions((String) request.get("deliveryInstructions"))
                .paymentMethod((String) request.get("paymentMethod"))
                .community(community)
                .build();

        return toResponse(subscriptionRepo.save(subscription));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMySubscriptions(Long communityId, Long userId) {
        return subscriptionRepo.findByUserIdAndCommunityId(userId, communityId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> pause(Long communityId, Long id, Map<String, Object> request) {
        LocalDate pauseStart = request.get("pauseStart") != null ? LocalDate.parse((String) request.get("pauseStart")) : null;
        LocalDate pauseEnd = request.get("pauseEnd") != null ? LocalDate.parse((String) request.get("pauseEnd")) : null;
        String reason = (String) request.get("reason");
        FoodSubscription subscription = subscriptionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", id));
        subscription.setStatus(FoodSubscription.SubscriptionStatus.PAUSED);
        subscriptionRepo.save(subscription);

        FoodSubscriptionPause pause = FoodSubscriptionPause.builder()
                .subscription(subscription)
                .pauseStart(pauseStart)
                .pauseEnd(pauseEnd)
                .reason(reason)
                .status(FoodSubscriptionPause.PauseStatus.ACTIVE)
                .build();
        pauseRepo.save(pause);

        return toResponse(subscription);
    }

    @Transactional
    public Map<String, Object> resume(Long communityId, Long id) {
        FoodSubscription subscription = subscriptionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", id));
        subscription.setStatus(FoodSubscription.SubscriptionStatus.ACTIVE);

        List<FoodSubscriptionPause> pauses = pauseRepo.findBySubscriptionId(id);
        pauses.stream()
                .filter(p -> p.getStatus() == FoodSubscriptionPause.PauseStatus.ACTIVE)
                .forEach(p -> {
                    p.setStatus(FoodSubscriptionPause.PauseStatus.COMPLETED);
                    pauseRepo.save(p);
                });

        return toResponse(subscriptionRepo.save(subscription));
    }

    @Transactional
    public Map<String, Object> cancel(Long communityId, Long id, Map<String, Object> request) {
        String reason = (String) request.get("reason");
        FoodSubscription subscription = subscriptionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", id));
        subscription.setStatus(FoodSubscription.SubscriptionStatus.CANCELLED);
        return toResponse(subscriptionRepo.save(subscription));
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getDeliveries(Long communityId, Long subscriptionId, Pageable pageable) {
        return deliveryRepo.findBySubscriptionId(subscriptionId, pageable)
                .map(d -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", d.getId());
                    map.put("subscriptionId", d.getSubscription().getId());
                    map.put("date", d.getDate());
                    map.put("mealType", d.getMealType());
                    map.put("status", d.getStatus() != null ? d.getStatus().name() : null);
                    map.put("deliveredAt", d.getDeliveredAt());
                    map.put("deliveryPartnerId", d.getDeliveryPartnerId());
                    map.put("feedbackRating", d.getFeedbackRating());
                    map.put("createdAt", d.getCreatedAt());
                    map.put("updatedAt", d.getUpdatedAt());
                    return map;
                });
    }

    private Map<String, Object> toResponse(FoodSubscriptionPlan plan) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", plan.getId());
        map.put("name", plan.getName());
        map.put("description", plan.getDescription());
        map.put("planType", plan.getPlanType() != null ? plan.getPlanType().name() : null);
        map.put("targetAudience", plan.getTargetAudience() != null ? plan.getTargetAudience().name() : null);
        map.put("providerType", plan.getProviderType() != null ? plan.getProviderType().name() : null);
        map.put("providerId", plan.getProviderId());
        map.put("pricePerMeal", plan.getPricePerMeal());
        map.put("monthlyPrice", plan.getMonthlyPrice());
        map.put("minDays", plan.getMinDays());
        map.put("includesWeekends", plan.getIncludesWeekends());
        map.put("active", plan.getActive());
        map.put("imageUrl", plan.getImageUrl());
        map.put("nutritionInfo", plan.getNutritionInfo());
        map.put("communityId", plan.getCommunity() != null ? plan.getCommunity().getId() : null);
        map.put("createdAt", plan.getCreatedAt());
        map.put("updatedAt", plan.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toResponse(FoodSubscription sub) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", sub.getId());
        map.put("planId", sub.getPlan() != null ? sub.getPlan().getId() : null);
        map.put("planName", sub.getPlan() != null ? sub.getPlan().getName() : null);
        map.put("userId", sub.getUser() != null ? sub.getUser().getId() : null);
        map.put("startDate", sub.getStartDate());
        map.put("endDate", sub.getEndDate());
        map.put("status", sub.getStatus() != null ? sub.getStatus().name() : null);
        map.put("autoRenew", sub.getAutoRenew());
        map.put("deliveryAddress", sub.getDeliveryAddress());
        map.put("deliveryInstructions", sub.getDeliveryInstructions());
        map.put("paymentMethod", sub.getPaymentMethod());
        map.put("communityId", sub.getCommunity() != null ? sub.getCommunity().getId() : null);
        map.put("createdAt", sub.getCreatedAt());
        map.put("updatedAt", sub.getUpdatedAt());
        return map;
    }
}
