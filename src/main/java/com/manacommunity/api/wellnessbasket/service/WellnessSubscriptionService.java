package com.manacommunity.api.wellnessbasket.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.freshkitchen.model.DailyBasket;
import com.manacommunity.api.freshkitchen.model.DailyBasketItem;
import com.manacommunity.api.freshkitchen.model.ExecutionMode;
import com.manacommunity.api.freshkitchen.model.FoodCategory;
import com.manacommunity.api.freshkitchen.model.FreshFoodProductConfig;
import com.manacommunity.api.freshkitchen.repository.DailyBasketItemRepository;
import com.manacommunity.api.freshkitchen.repository.DailyBasketRepository;
import com.manacommunity.api.freshkitchen.repository.FreshFoodProductConfigRepository;
import com.manacommunity.api.freshkitchen.service.DailyBasketService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.wellnessbasket.dto.*;
import com.manacommunity.api.wellnessbasket.entity.WellnessCommunityCluster;
import com.manacommunity.api.wellnessbasket.entity.WellnessSubscriberPreference;
import com.manacommunity.api.wellnessbasket.repository.WellnessSubscriberPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WellnessSubscriptionService {

    private final WellnessSubscriberPreferenceRepository subscriberPrefRepository;
    private final WellnessClusterService clusterService;
    private final WellnessMenuRotationService menuRotationService;
    private final DailyBasketService dailyBasketService;
    private final DailyBasketRepository basketRepository;
    private final DailyBasketItemRepository basketItemRepository;
    private final FreshFoodProductConfigRepository productConfigRepository;
    private final AppUserRepository userRepository;
    private final CommunityRepository communityRepository;

    public List<WellnessPlanDto> getAvailablePlans() {
        return List.of(
                WellnessPlanDto.builder()
                        .planType("BASIC")
                        .displayName("Basic Wellness Box")
                        .description("Daily whole-food breakfast kit designed for clean energy and gut health.")
                        .pricePerMonth(new BigDecimal("1999.00"))
                        .pricePerDelivery(new BigDecimal("77.00"))
                        .deliveriesPerMonth(26)
                        .includedFeatures(List.of(
                                "2 Seasonal Fruit portions",
                                "1 Ancient Millet / Grain breakfast kit",
                                "1 Fresh-cut Micro-Salad / Vegetable portion",
                                "Dietitian-reviewed 4-week rotation",
                                "Doorstep delivery before 8:00 AM (Mon-Sat)"
                        ))
                        .boxContentsSummary(List.of("Seasonal Fruits", "Millet Kit", "Greens Bowl"))
                        .build(),
                WellnessPlanDto.builder()
                        .planType("PREMIUM")
                        .displayName("Premium Wellness Box")
                        .description("High-protein & superfood breakfast kit with personalised macro tracking.")
                        .pricePerMonth(new BigDecimal("2799.00"))
                        .pricePerDelivery(new BigDecimal("108.00"))
                        .deliveriesPerMonth(26)
                        .includedFeatures(List.of(
                                "2 Seasonal & Exotic Fruit portions",
                                "1 High-Protein / Steamed Sprout portion",
                                "1 Millet / Ancient Grain Kit",
                                "1 Fresh-cut salad with homemade dressing",
                                "1 Superfood Add-on (Chia/Nuts/Flax Seeds)",
                                "Automated allergy substitution guarantee",
                                "Dietitian-reviewed 4-week rotation",
                                "Priority doorstep delivery before 8:00 AM (Mon-Sat)"
                        ))
                        .boxContentsSummary(List.of("Exotic Fruits", "Sprout/Protein Bowl", "Millet Kit", "Salad", "Superfood Mix"))
                        .build()
        );
    }

    @Transactional
    public WellnessSubscriberDto subscribe(Long userId, WellnessSubscribeRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", userId));

        // Get or create cluster for the society
        String society = request.getSocietyName() != null ? request.getSocietyName() : "Mana Premiere Residences";
        WellnessCommunityCluster cluster = clusterService.getOrCreateCluster(society, "Hyderabad", request.getBlock());

        Optional<WellnessSubscriberPreference> existingOpt = subscriberPrefRepository.findByUserId(userId);
        boolean isNew = existingOpt.isEmpty() || !"ACTIVE".equalsIgnoreCase(existingOpt.get().getSubscriptionStatus());

        WellnessSubscriberPreference pref = existingOpt.orElseGet(() -> {
            WellnessSubscriberPreference newPref = new WellnessSubscriberPreference();
            newPref.setUserId(userId);
            return newPref;
        });

        pref.setPlanType(request.getPlanType() != null ? request.getPlanType().toUpperCase() : "BASIC");
        pref.setPortionSize(request.getPortionSize() != null ? request.getPortionSize().toUpperCase() : "STANDARD");
        pref.setGoalType(request.getGoalType());
        if (request.getDislikedIngredients() != null) {
            pref.setDislikedIngredients(request.getDislikedIngredients().toArray(new String[0]));
        }
        pref.setExtraNotes(request.getExtraNotes());
        pref.setSubscriptionStatus("ACTIVE");
        pref.setPauseFrom(null);
        pref.setPauseUntil(null);
        pref.setCluster(cluster);
        pref.setCashfreeSubscriptionId("CF-SUB-MOCK-" + UUID.randomUUID().toString().substring(0, 8));

        WellnessSubscriberPreference saved = subscriberPrefRepository.save(pref);

        if (isNew) {
            clusterService.incrementSubscriberCount(cluster.getId());
        }

        // Sync or initialize DailyBasket for this user
        Community community = communityRepository.findAll().stream().findFirst().orElse(null);
        if (community != null) {
            DailyBasket basket = basketRepository.findByUserIdAndCommunityId(userId, community.getId())
                    .orElseGet(() -> {
                        DailyBasket b = new DailyBasket();
                        b.setUser(user);
                        b.setCommunity(community);
                        b.setIsActive(true);
                        b.setIsPaused(false);
                        b.setDeliverySlot("MORNING_06_00_08_00");
                        String addr = (user.getFlatNo() != null ? "Flat " + user.getFlatNo() : "")
                                + (user.getTower() != null ? ", " + user.getTower() : "")
                                + (user.getBlock() != null ? ", " + user.getBlock() : "");
                        b.setDeliveryAddress(addr.isBlank() ? "Apartment " + society : addr);
                        return basketRepository.save(b);
                    });
            syncWellnessItemsIntoDailyBasket(basket, saved.getPlanType());
        }

        log.info("User {} subscribed to Wellness plan {} in cluster {}", userId, saved.getPlanType(), cluster.getSocietyName());
        return toSubscriberDto(saved);
    }

    private void syncWellnessItemsIntoDailyBasket(DailyBasket basket, String planType) {
        // Clear old wellness items in basket
        List<DailyBasketItem> items = basketItemRepository.findByBasketId(basket.getId());
        List<DailyBasketItem> wellnessItems = items.stream()
                .filter(i -> i.getCategory() == FoodCategory.HEALTHY_BREAKFAST)
                .toList();
        basketItemRepository.deleteAll(wellnessItems);

        // Find or create default wellness product config
        String prodId = "PREMIUM".equalsIgnoreCase(planType) ? "WB-PREMIUM-BOX" : "WB-BASIC-BOX";
        String prodName = "PREMIUM".equalsIgnoreCase(planType) ? "Premium Wellness Basket" : "Basic Wellness Basket";
        BigDecimal price = "PREMIUM".equalsIgnoreCase(planType) ? new BigDecimal("108.00") : new BigDecimal("77.00");

        FreshFoodProductConfig config = productConfigRepository.findByProductId(prodId)
                .orElseGet(() -> {
                    FreshFoodProductConfig c = new FreshFoodProductConfig();
                    c.setProductId(prodId);
                    c.setName(prodName);
                    c.setCategory(FoodCategory.HEALTHY_BREAKFAST);
                    c.setExecutionMode(ExecutionMode.DAILY_BASKET);
                    c.setUnit("BOX");
                    c.setPackSize("1 Daily Portion");
                    c.setUnitPrice(price);
                    c.setShelfLifeHours(24);
                    c.setIsActive(true);
                    return productConfigRepository.save(c);
                });

        DailyBasketItem item = DailyBasketItem.builder()
                .basket(basket)
                .productId(config.getProductId())
                .productName(config.getName())
                .quantity(1)
                .unitPrice(config.getUnitPrice())
                .category(FoodCategory.HEALTHY_BREAKFAST)
                .executionMode(ExecutionMode.DAILY_BASKET)
                .mon(true).tue(true).wed(true).thu(true).fri(true).sat(true).sun(false) // 6 days Mon-Sat
                .isActive(true)
                .build();
        basketItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public WellnessSubscriberDto getSubscription(Long userId) {
        return subscriberPrefRepository.findByUserId(userId)
                .map(this::toSubscriberDto)
                .orElseThrow(() -> new ResourceNotFoundException("Wellness subscription for user " + userId));
    }

    @Transactional
    public WellnessSubscriberDto pauseSubscription(Long userId, WellnessPauseRequest request) {
        WellnessSubscriberPreference pref = subscriberPrefRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wellness subscription for user " + userId));

        if (request.getPauseUntil().isBefore(request.getPauseFrom())) {
            throw new InvalidInputException("pauseUntil date cannot be before pauseFrom date");
        }

        pref.setSubscriptionStatus("PAUSED");
        pref.setPauseFrom(request.getPauseFrom());
        pref.setPauseUntil(request.getPauseUntil());
        WellnessSubscriberPreference saved = subscriberPrefRepository.save(pref);

        // Pause underlying daily basket if present
        Community community = communityRepository.findAll().stream().findFirst().orElse(null);
        if (community != null) {
            basketRepository.findByUserIdAndCommunityId(userId, community.getId()).ifPresent(b -> {
                b.setIsPaused(true);
                b.setPauseStartDate(request.getPauseFrom());
                b.setPauseEndDate(request.getPauseUntil());
                basketRepository.save(b);
            });
        }

        log.info("Wellness subscription for user {} paused from {} to {}", userId, request.getPauseFrom(), request.getPauseUntil());
        return toSubscriberDto(saved);
    }

    @Transactional
    public WellnessSubscriberDto resumeSubscription(Long userId) {
        WellnessSubscriberPreference pref = subscriberPrefRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wellness subscription for user " + userId));

        pref.setSubscriptionStatus("ACTIVE");
        pref.setPauseFrom(null);
        pref.setPauseUntil(null);
        WellnessSubscriberPreference saved = subscriberPrefRepository.save(pref);

        Community community = communityRepository.findAll().stream().findFirst().orElse(null);
        if (community != null) {
            basketRepository.findByUserIdAndCommunityId(userId, community.getId()).ifPresent(b -> {
                b.setIsPaused(false);
                b.setPauseStartDate(null);
                b.setPauseEndDate(null);
                basketRepository.save(b);
            });
        }

        log.info("Wellness subscription for user {} resumed to ACTIVE", userId);
        return toSubscriberDto(saved);
    }

    @Transactional
    public void cancelSubscription(Long userId) {
        WellnessSubscriberPreference pref = subscriberPrefRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wellness subscription for user " + userId));

        if ("ACTIVE".equalsIgnoreCase(pref.getSubscriptionStatus()) && pref.getCluster() != null) {
            clusterService.decrementSubscriberCount(pref.getCluster().getId());
        }

        pref.setSubscriptionStatus("CANCELLED");
        subscriberPrefRepository.save(pref);

        // Remove wellness items from daily basket
        Community community = communityRepository.findAll().stream().findFirst().orElse(null);
        if (community != null) {
            basketRepository.findByUserIdAndCommunityId(userId, community.getId()).ifPresent(b -> {
                List<DailyBasketItem> items = basketItemRepository.findByBasketId(b.getId());
                List<DailyBasketItem> wellnessItems = items.stream()
                        .filter(i -> i.getCategory() == FoodCategory.HEALTHY_BREAKFAST)
                        .toList();
                basketItemRepository.deleteAll(wellnessItems);
            });
        }

        log.info("Wellness subscription for user {} cancelled", userId);
    }

    /**
     * Nightly wellness engine:
     * Generates daily wellness orders for tomorrow (if Mon-Sat).
     * Delegates to DailyBasketService with idempotent order generation.
     */
    @Transactional
    public int generateDailyWellnessBaskets(LocalDate targetDate) {
        LocalDate date = targetDate != null ? targetDate : LocalDate.now().plusDays(1);
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            log.info("Target date {} is Sunday. Wellness boxes deliver Mon-Sat. Skipping generation.", date);
            return 0;
        }

        log.info("Triggering authoritative order generation for wellness subscribers for {}", date);
        return dailyBasketService.generateDailyOrders(date);
    }

    private WellnessSubscriberDto toSubscriberDto(WellnessSubscriberPreference pref) {
        WellnessCommunityCluster cluster = pref.getCluster();
        return WellnessSubscriberDto.builder()
                .id(pref.getId())
                .userId(pref.getUserId())
                .planType(pref.getPlanType())
                .portionSize(pref.getPortionSize())
                .goalType(pref.getGoalType())
                .dislikedIngredients(pref.getDislikedIngredients() != null ? Arrays.asList(pref.getDislikedIngredients()) : Collections.emptyList())
                .extraNotes(pref.getExtraNotes())
                .subscriptionStatus(pref.getSubscriptionStatus())
                .pauseFrom(pref.getPauseFrom())
                .pauseUntil(pref.getPauseUntil())
                .cashfreeSubscriptionId(pref.getCashfreeSubscriptionId())
                .clusterId(cluster != null ? cluster.getId() : null)
                .clusterSocietyName(cluster != null ? cluster.getSocietyName() : null)
                .clusterStatus(cluster != null ? cluster.getStatus() : null)
                .clusterCurrentSubscribers(cluster != null ? cluster.getCurrentSubscriberCount() : 0)
                .clusterActivationThreshold(cluster != null ? cluster.getActivationThreshold() : 15)
                .createdAt(pref.getCreatedAt())
                .build();
    }
}
