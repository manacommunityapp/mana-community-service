package com.manacommunity.api.unit.service;

import com.manacommunity.api.food.entity.FoodResidentAllergy;
import com.manacommunity.api.food.entity.FoodResidentAllergy.Severity;
import com.manacommunity.api.food.entity.FoodResidentProfile;
import com.manacommunity.api.food.repository.FoodResidentAllergyRepository;
import com.manacommunity.api.food.repository.FoodResidentProfileRepository;
import com.manacommunity.api.freshkitchen.model.DailyBasket;
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
import com.manacommunity.api.wellnessbasket.entity.*;
import com.manacommunity.api.wellnessbasket.repository.*;
import com.manacommunity.api.wellnessbasket.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WellnessBasketServiceTest {

    @Mock
    private WellnessSubscriberPreferenceRepository subscriberPrefRepository;
    @Mock
    private WellnessCommunityClusterRepository clusterRepository;
    @Mock
    private WellnessMenuCycleRepository menuCycleRepository;
    @Mock
    private WellnessProductNutritionRepository nutritionRepository;
    @Mock
    private WellnessMenuSwapRepository swapRepository;
    @Mock
    private FreshFoodProductConfigRepository productConfigRepository;
    @Mock
    private FoodResidentProfileRepository foodProfileRepository;
    @Mock
    private FoodResidentAllergyRepository allergyRepository;
    @Mock
    private DailyBasketRepository basketRepository;
    @Mock
    private DailyBasketItemRepository basketItemRepository;
    @Mock
    private AppUserRepository userRepository;
    @Mock
    private CommunityRepository communityRepository;
    @Mock
    private DailyBasketService dailyBasketService;

    private WellnessClusterService clusterService;
    private WellnessMenuRotationService menuRotationService;
    private WellnessSubscriptionService subscriptionService;

    private AppUser testUser;
    private WellnessCommunityCluster testCluster;

    @BeforeEach
    void setUp() {
        clusterService = new WellnessClusterService(clusterRepository);
        menuRotationService = new WellnessMenuRotationService(
                menuCycleRepository,
                nutritionRepository,
                swapRepository,
                subscriberPrefRepository,
                productConfigRepository,
                foodProfileRepository,
                allergyRepository
        );
        subscriptionService = new WellnessSubscriptionService(
                subscriberPrefRepository,
                clusterService,
                menuRotationService,
                dailyBasketService,
                basketRepository,
                basketItemRepository,
                productConfigRepository,
                userRepository,
                communityRepository
        );

        testUser = new AppUser();
        testUser.setId(101L);
        testUser.setFlatNo("A-402");
        testUser.setTower("Tower 1");
        testUser.setBlock("Phase 1");

        testCluster = new WellnessCommunityCluster();
        testCluster.setId(1L);
        testCluster.setSocietyName("Mana Premiere Residences");
        testCluster.setStatus("PENDING");
        testCluster.setCurrentSubscriberCount(14);
        testCluster.setActivationThreshold(15);
        testCluster.setMaxCapacity(80);
    }

    @Test
    void testGetAvailablePlans_ReturnsBasicAndPremiumWith26Deliveries() {
        List<WellnessPlanDto> plans = subscriptionService.getAvailablePlans();

        assertEquals(2, plans.size());

        WellnessPlanDto basic = plans.stream().filter(p -> "BASIC".equals(p.getPlanType())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("1999.00"), basic.getPricePerMonth());
        assertEquals(new BigDecimal("77.00"), basic.getPricePerDelivery());
        assertEquals(26, basic.getDeliveriesPerMonth());

        WellnessPlanDto premium = plans.stream().filter(p -> "PREMIUM".equals(p.getPlanType())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("2799.00"), premium.getPricePerMonth());
        assertEquals(new BigDecimal("108.00"), premium.getPricePerDelivery());
        assertEquals(26, premium.getDeliveriesPerMonth());
    }

    @Test
    void testClusterActivation_FlipsToActiveAtThreshold() {
        when(clusterRepository.findById(1L)).thenReturn(Optional.of(testCluster));
        when(clusterRepository.save(any(WellnessCommunityCluster.class))).thenAnswer(i -> i.getArgument(0));

        // When 15th subscriber joins, count becomes 15 and status flips from PENDING to ACTIVE
        clusterService.incrementSubscriberCount(1L);

        assertEquals(15, testCluster.getCurrentSubscriberCount());
        assertEquals("ACTIVE", testCluster.getStatus());
        verify(clusterRepository).save(testCluster);
    }

    @Test
    void testSubscribe_CreatesActivePreferenceAndSyncsBasket() {
        when(userRepository.findById(101L)).thenReturn(Optional.of(testUser));
        when(clusterRepository.findBySocietyNameIgnoreCase("Mana Premiere Residences")).thenReturn(Optional.of(testCluster));
        when(subscriberPrefRepository.findByUserId(101L)).thenReturn(Optional.empty());
        when(subscriberPrefRepository.save(any(WellnessSubscriberPreference.class))).thenAnswer(inv -> {
            WellnessSubscriberPreference p = inv.getArgument(0);
            p.setId(55L);
            return p;
        });
        when(clusterRepository.findById(1L)).thenReturn(Optional.of(testCluster));

        WellnessSubscribeRequest request = WellnessSubscribeRequest.builder()
                .planType("PREMIUM")
                .portionSize("STANDARD")
                .goalType("WEIGHT_LOSS")
                .societyName("Mana Premiere Residences")
                .build();

        WellnessSubscriberDto dto = subscriptionService.subscribe(101L, request);

        assertNotNull(dto);
        assertEquals("PREMIUM", dto.getPlanType());
        assertEquals("ACTIVE", dto.getSubscriptionStatus());
        verify(subscriberPrefRepository).save(any(WellnessSubscriberPreference.class));
    }

    @Test
    void testPauseSubscription_UpdatesDatesAndSetsPaused() {
        WellnessSubscriberPreference pref = new WellnessSubscriberPreference();
        pref.setId(55L);
        pref.setUserId(101L);
        pref.setPlanType("BASIC");
        pref.setSubscriptionStatus("ACTIVE");

        when(subscriberPrefRepository.findByUserId(101L)).thenReturn(Optional.of(pref));
        when(subscriberPrefRepository.save(any(WellnessSubscriberPreference.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(101L)).thenReturn(Optional.of(testUser));

        LocalDate from = LocalDate.now().plusDays(2);
        LocalDate to = LocalDate.now().plusDays(7);
        WellnessPauseRequest pauseReq = WellnessPauseRequest.builder()
                .pauseFrom(from)
                .pauseUntil(to)
                .build();

        WellnessSubscriberDto result = subscriptionService.pauseSubscription(101L, pauseReq);

        assertEquals("PAUSED", result.getSubscriptionStatus());
        assertEquals(from, result.getPauseFrom());
        assertEquals(to, result.getPauseUntil());
    }

    @Test
    void testMenuRotation_AutoSubstitutesAllergenConflict() {
        Long almondMilkId = 10L;
        Long oatMilkId = 20L;

        // User has NUT allergy
        FoodResidentProfile profile = new FoodResidentProfile();
        profile.setId(1L);
        profile.setUser(testUser);

        FoodResidentAllergy nutAllergy = new FoodResidentAllergy();
        nutAllergy.setAllergyName("NUT");
        nutAllergy.setSeverity(Severity.SEVERE);

        when(foodProfileRepository.findByUserId(101L)).thenReturn(List.of(profile));
        when(allergyRepository.findByProfileId(1L)).thenReturn(List.of(nutAllergy));

        // Menu cycle contains almond milk (ID 10) with oat milk as alt (ID 20)
        WellnessMenuCycle cycle = new WellnessMenuCycle();
        cycle.setPlanType("BASIC");
        cycle.setCycleWeek((short) 1);
        cycle.setDayOfWeek((short) 1);
        cycle.setProductIds(new Long[]{almondMilkId});
        cycle.setAltProductIds(new Long[]{oatMilkId});

        when(menuCycleRepository.findByPlanTypeAndCycleWeekAndDayOfWeek(anyString(), anyShort(), anyShort()))
                .thenReturn(Optional.of(cycle));

        // Nutrition data: almond milk has allergen NUT; oat milk has no allergens
        WellnessProductNutrition almondNut = new WellnessProductNutrition();
        almondNut.setProductId(almondMilkId);
        almondNut.setEnergyKcal(new BigDecimal("120"));
        almondNut.setProteinG(new BigDecimal("3"));
        almondNut.setCarbsG(new BigDecimal("8"));
        almondNut.setFatG(new BigDecimal("7"));
        almondNut.setAllergenFlags(new String[]{"NUT"});

        WellnessProductNutrition oatNut = new WellnessProductNutrition();
        oatNut.setProductId(oatMilkId);
        oatNut.setEnergyKcal(new BigDecimal("130"));
        oatNut.setProteinG(new BigDecimal("4"));
        oatNut.setCarbsG(new BigDecimal("18"));
        oatNut.setFatG(new BigDecimal("3"));
        oatNut.setAllergenFlags(new String[]{});

        when(nutritionRepository.findByProductId(almondMilkId)).thenReturn(Optional.of(almondNut));
        when(nutritionRepository.findByProductId(oatMilkId)).thenReturn(Optional.of(oatNut));

        FreshFoodProductConfig almondConfig = FreshFoodProductConfig.builder().id(almondMilkId).name("Almond Milk Bowl").category(FoodCategory.HEALTHY_BREAKFAST).build();
        FreshFoodProductConfig oatConfig = FreshFoodProductConfig.builder().id(oatMilkId).name("Oat Milk Bowl").category(FoodCategory.HEALTHY_BREAKFAST).build();
        lenient().when(productConfigRepository.findById(almondMilkId)).thenReturn(Optional.of(almondConfig));
        when(productConfigRepository.findById(oatMilkId)).thenReturn(Optional.of(oatConfig));

        WellnessMenuWeekDto week = menuRotationService.getMenuForWeek(101L, LocalDate.of(2026, 11, 2));

        assertNotNull(week);
        WellnessMenuDayDto monday = week.getDays().get(0);
        assertEquals(1, monday.getProducts().size());
        // Assert that oat milk (safe alternative) was substituted instead of almond milk!
        assertEquals(oatMilkId, monday.getProducts().get(0).getProductId());
        assertEquals("Oat Milk Bowl", monday.getProducts().get(0).getProductName());
    }
}
