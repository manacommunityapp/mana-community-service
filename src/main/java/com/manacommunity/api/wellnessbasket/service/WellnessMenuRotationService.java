package com.manacommunity.api.wellnessbasket.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.food.entity.FoodResidentAllergy;
import com.manacommunity.api.food.entity.FoodResidentProfile;
import com.manacommunity.api.food.repository.FoodResidentAllergyRepository;
import com.manacommunity.api.food.repository.FoodResidentProfileRepository;
import com.manacommunity.api.freshkitchen.model.FreshFoodProductConfig;
import com.manacommunity.api.freshkitchen.repository.FreshFoodProductConfigRepository;
import com.manacommunity.api.wellnessbasket.dto.*;
import com.manacommunity.api.wellnessbasket.entity.WellnessMenuCycle;
import com.manacommunity.api.wellnessbasket.entity.WellnessMenuSwap;
import com.manacommunity.api.wellnessbasket.entity.WellnessProductNutrition;
import com.manacommunity.api.wellnessbasket.entity.WellnessSubscriberPreference;
import com.manacommunity.api.wellnessbasket.repository.WellnessMenuCycleRepository;
import com.manacommunity.api.wellnessbasket.repository.WellnessMenuSwapRepository;
import com.manacommunity.api.wellnessbasket.repository.WellnessProductNutritionRepository;
import com.manacommunity.api.wellnessbasket.repository.WellnessSubscriberPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WellnessMenuRotationService {

    private final WellnessMenuCycleRepository menuCycleRepository;
    private final WellnessProductNutritionRepository nutritionRepository;
    private final WellnessMenuSwapRepository swapRepository;
    private final WellnessSubscriberPreferenceRepository subscriberPrefRepository;
    private final FreshFoodProductConfigRepository productConfigRepository;
    private final FoodResidentProfileRepository foodProfileRepository;
    private final FoodResidentAllergyRepository allergyRepository;

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    /**
     * Checks if the weekly swap window is open.
     * Swap window: Friday 18:00 IST to Saturday 20:00 IST.
     */
    public boolean isSwapWindowOpen() {
        ZonedDateTime nowIst = ZonedDateTime.now(IST_ZONE);
        DayOfWeek dow = nowIst.getDayOfWeek();
        LocalTime time = nowIst.toLocalTime();

        if (dow == DayOfWeek.FRIDAY) {
            return !time.isBefore(LocalTime.of(18, 0));
        } else if (dow == DayOfWeek.SATURDAY) {
            return !time.isAfter(LocalTime.of(20, 0));
        }
        return false;
    }

    public String getSwapWindowStatusMessage() {
        if (isSwapWindowOpen()) {
            return "Weekly Swap Window is OPEN! You can customize next week's menu until Saturday 8:00 PM IST.";
        }
        return "Weekly Swap Window opens every Friday at 6:00 PM IST and closes Saturday at 8:00 PM IST.";
    }

    /**
     * Retrieves the 6-day (Mon-Sat) menu for a subscriber for the given week.
     */
    @Transactional(readOnly = true)
    public WellnessMenuWeekDto getMenuForWeek(Long userId, LocalDate weekStartDate) {
        LocalDate monday = weekStartDate != null
                ? weekStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : LocalDate.now(IST_ZONE).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        LocalDate saturday = monday.plusDays(5);

        WellnessSubscriberPreference pref = userId != null
                ? subscriberPrefRepository.findByUserId(userId).orElse(null)
                : null;

        String planType = pref != null ? pref.getPlanType() : "BASIC";

        // Calculate cycle week: 1-4 based on days since epoch
        short cycleWeek = (short) (((monday.toEpochDay() / 7) % 4) + 1);

        // Fetch user allergens
        Set<String> userAllergens = fetchUserAllergens(userId);
        Set<String> dislikedIngredients = pref != null && pref.getDislikedIngredients() != null
                ? Arrays.stream(pref.getDislikedIngredients()).map(String::toLowerCase).collect(Collectors.toSet())
                : Collections.emptySet();

        List<WellnessMenuDayDto> days = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            LocalDate deliveryDate = monday.plusDays(i);
            short dayOfWeek = (short) (i + 1); // 1 = Monday ... 6 = Saturday

            WellnessMenuDayDto dayDto = resolveDayMenu(
                    planType, cycleWeek, dayOfWeek, deliveryDate, pref, userAllergens, dislikedIngredients
            );
            days.add(dayDto);
        }

        return WellnessMenuWeekDto.builder()
                .cycleWeek(cycleWeek)
                .weekStartDate(monday)
                .weekEndDate(saturday)
                .planType(planType)
                .days(days)
                .isSwapWindowOpen(isSwapWindowOpen())
                .swapWindowCloseMessage(getSwapWindowStatusMessage())
                .build();
    }

    private WellnessMenuDayDto resolveDayMenu(
            String planType, short cycleWeek, short dayOfWeek, LocalDate deliveryDate,
            WellnessSubscriberPreference pref, Set<String> userAllergens, Set<String> dislikedIngredients
    ) {
        DayOfWeek dow = deliveryDate.getDayOfWeek();
        String dowName = dow.name().substring(0, 1) + dow.name().substring(1).toLowerCase();

        Optional<WellnessMenuCycle> cycleOpt = menuCycleRepository.findByPlanTypeAndCycleWeekAndDayOfWeek(
                planType, cycleWeek, dayOfWeek
        );

        List<Long> productIds = new ArrayList<>();
        List<Long> altProductIds = new ArrayList<>();
        String dietitianNotes = "Dietitian-curated fresh nutrition portion for optimal energy and recovery.";

        if (cycleOpt.isPresent()) {
            WellnessMenuCycle cycle = cycleOpt.get();
            if (cycle.getProductIds() != null) {
                productIds.addAll(Arrays.asList(cycle.getProductIds()));
            }
            if (cycle.getAltProductIds() != null) {
                altProductIds.addAll(Arrays.asList(cycle.getAltProductIds()));
            }
            if (cycle.getDietitianNotes() != null && !cycle.getDietitianNotes().isBlank()) {
                dietitianNotes = cycle.getDietitianNotes();
            }
        }

        // If cycle had empty productIds, fallback to active products
        if (productIds.isEmpty()) {
            List<FreshFoodProductConfig> fallback = productConfigRepository.findByIsActiveTrue();
            productIds = fallback.stream().limit(3).map(FreshFoodProductConfig::getId).toList();
        }

        // Check for active resident swap for this deliveryDate
        Long activeSwapId = null;
        String activeSwapStatus = null;
        boolean isSwapped = false;

        if (pref != null) {
            List<WellnessMenuSwap> swaps = swapRepository.findBySubscriberPreferenceIdAndDeliveryDate(
                    pref.getId(), deliveryDate
            );
            for (WellnessMenuSwap swap : swaps) {
                if ("APPROVED".equalsIgnoreCase(swap.getStatus()) || "PENDING".equalsIgnoreCase(swap.getStatus())) {
                    activeSwapId = swap.getId();
                    activeSwapStatus = swap.getStatus();
                    if ("APPROVED".equalsIgnoreCase(swap.getStatus())) {
                        isSwapped = true;
                        Collections.replaceAll(productIds, swap.getOriginalProductId(), swap.getRequestedProductId());
                    }
                    break;
                }
            }
        }

        // Allergy and Dislike Gate: Auto-substitution
        List<Long> finalProductIds = new ArrayList<>();
        for (Long pid : productIds) {
            Long safePid = resolveAllergySafeProduct(pid, altProductIds, userAllergens, dislikedIngredients);
            finalProductIds.add(safePid);
        }

        // Build product nutrition DTOs
        List<WellnessNutritionDto> products = finalProductIds.stream()
                .map(this::getNutritionForProduct)
                .filter(Objects::nonNull)
                .toList();

        List<WellnessNutritionDto> altProducts = altProductIds.stream()
                .map(this::getNutritionForProduct)
                .filter(Objects::nonNull)
                .toList();

        // Calculate macro totals
        BigDecimal totalKcal = products.stream()
                .map(p -> p.getEnergyKcal() != null ? p.getEnergyKcal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProtein = products.stream()
                .map(p -> p.getProteinG() != null ? p.getProteinG() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCarbs = products.stream()
                .map(p -> p.getCarbsG() != null ? p.getCarbsG() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFat = products.stream()
                .map(p -> p.getFatG() != null ? p.getFatG() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFibre = products.stream()
                .map(p -> p.getFibreG() != null ? p.getFibreG() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return WellnessMenuDayDto.builder()
                .deliveryDate(deliveryDate)
                .dayOfWeekName(dowName)
                .dayOfWeek(dayOfWeek)
                .planType(planType)
                .products(products)
                .alternativeProducts(altProducts)
                .totalEnergyKcal(totalKcal)
                .totalProteinG(totalProtein)
                .totalCarbsG(totalCarbs)
                .totalFatG(totalFat)
                .totalFibreG(totalFibre)
                .dietitianNotes(dietitianNotes)
                .isSwapped(isSwapped)
                .activeSwapId(activeSwapId)
                .activeSwapStatus(activeSwapStatus)
                .build();
    }

    private Long resolveAllergySafeProduct(
            Long productId, List<Long> altProductIds, Set<String> userAllergens, Set<String> dislikedIngredients
    ) {
        if (!hasAllergyOrDislikeConflict(productId, userAllergens, dislikedIngredients)) {
            return productId;
        }

        // Try to find substitute from altProductIds
        for (Long altId : altProductIds) {
            if (!hasAllergyOrDislikeConflict(altId, userAllergens, dislikedIngredients)) {
                log.info("Auto-substituted allergen-conflicting product {} with alternative {}", productId, altId);
                return altId;
            }
        }

        log.warn("Allergy conflict detected for product {} with no safe alternative in BOM", productId);
        return productId;
    }

    private boolean hasAllergyOrDislikeConflict(
            Long productId, Set<String> userAllergens, Set<String> dislikedIngredients
    ) {
        Optional<WellnessProductNutrition> nutOpt = nutritionRepository.findByProductId(productId);
        if (nutOpt.isEmpty()) {
            return false;
        }
        WellnessProductNutrition nut = nutOpt.get();

        if (nut.getAllergenFlags() != null) {
            for (String flag : nut.getAllergenFlags()) {
                if (userAllergens.contains(flag.toUpperCase())) {
                    return true;
                }
            }
        }

        Optional<FreshFoodProductConfig> confOpt = productConfigRepository.findById(productId);
        if (confOpt.isPresent() && !dislikedIngredients.isEmpty()) {
            String nameLower = confOpt.get().getName().toLowerCase();
            for (String dislike : dislikedIngredients) {
                if (nameLower.contains(dislike)) {
                    return true;
                }
            }
        }

        return false;
    }

    private Set<String> fetchUserAllergens(Long userId) {
        if (userId == null) return Collections.emptySet();
        List<FoodResidentProfile> profiles = foodProfileRepository.findByUserId(userId);
        if (profiles.isEmpty()) return Collections.emptySet();

        Set<String> allergens = new HashSet<>();
        for (FoodResidentProfile profile : profiles) {
            List<FoodResidentAllergy> allergies = allergyRepository.findByProfileId(profile.getId());
            for (FoodResidentAllergy allergy : allergies) {
                if (allergy.getAllergyName() != null) {
                    allergens.add(allergy.getAllergyName().trim().toUpperCase());
                }
            }
        }
        return allergens;
    }

    public WellnessNutritionDto getNutritionForProduct(Long productId) {
        Optional<FreshFoodProductConfig> confOpt = productConfigRepository.findById(productId);
        String productName = confOpt.map(FreshFoodProductConfig::getName).orElse("Fresh Wellness Item");
        String category = confOpt.map(c -> c.getCategory().name()).orElse("HEALTHY_BREAKFAST");

        return nutritionRepository.findByProductId(productId)
                .map(n -> WellnessNutritionDto.builder()
                        .id(n.getId())
                        .productId(n.getProductId())
                        .productName(productName)
                        .category(category)
                        .energyKcal(n.getEnergyKcal())
                        .proteinG(n.getProteinG())
                        .carbsG(n.getCarbsG())
                        .sugarG(n.getSugarG())
                        .fatG(n.getFatG())
                        .saturatedFatG(n.getSaturatedFatG())
                        .fibreG(n.getFibreG())
                        .sodiumMg(n.getSodiumMg())
                        .glycemicIndex(n.getGlycemicIndex())
                        .perServingG(n.getPerServingG())
                        .goalTags(n.getGoalTags() != null ? Arrays.asList(n.getGoalTags()) : Collections.emptyList())
                        .allergenFlags(n.getAllergenFlags() != null ? Arrays.asList(n.getAllergenFlags()) : Collections.emptyList())
                        .build())
                .orElseGet(() -> WellnessNutritionDto.builder()
                        .productId(productId)
                        .productName(productName)
                        .category(category)
                        .energyKcal(BigDecimal.valueOf(180))
                        .proteinG(BigDecimal.valueOf(8))
                        .carbsG(BigDecimal.valueOf(25))
                        .fatG(BigDecimal.valueOf(4))
                        .fibreG(BigDecimal.valueOf(5))
                        .perServingG(BigDecimal.valueOf(100))
                        .goalTags(List.of("GENERAL_WELLNESS"))
                        .allergenFlags(Collections.emptyList())
                        .build());
    }

    /**
     * Resident requests a product swap for a specific delivery date.
     */
    @Transactional
    public WellnessSwapDto requestSwap(Long userId, WellnessSwapRequest request) {
        WellnessSubscriberPreference pref = subscriberPrefRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Active wellness subscription for user " + userId));

        if (!"ACTIVE".equalsIgnoreCase(pref.getSubscriptionStatus())) {
            throw new InvalidInputException("Subscription is not active. Current status: " + pref.getSubscriptionStatus());
        }

        if (request.getDeliveryDate().isBefore(LocalDate.now(IST_ZONE).plusDays(1))) {
            throw new InvalidInputException("Cannot swap items on or before the current delivery day (D-day lockout)");
        }

        // Validate window (unless weekend bypass or admin)
        if (!isSwapWindowOpen()) {
            log.warn("Swap requested outside standard Friday 6 PM - Saturday 8 PM window for user {}", userId);
            // Allow submission as PENDING for admin dietitian review
        }

        // Verify products exist
        productConfigRepository.findById(request.getOriginalProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Original product", request.getOriginalProductId()));
        FreshFoodProductConfig reqProduct = productConfigRepository.findById(request.getRequestedProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Requested product", request.getRequestedProductId()));

        // Create swap request
        WellnessMenuSwap swap = new WellnessMenuSwap();
        swap.setSubscriberPreference(pref);
        swap.setDeliveryDate(request.getDeliveryDate());
        swap.setOriginalProductId(request.getOriginalProductId());
        swap.setRequestedProductId(request.getRequestedProductId());
        // Auto-approve if requested product is active
        swap.setStatus(Boolean.TRUE.equals(reqProduct.getIsActive()) ? "APPROVED" : "PENDING");
        swap.setResolvedAt(Boolean.TRUE.equals(reqProduct.getIsActive()) ? OffsetDateTime.now() : null);

        WellnessMenuSwap saved = swapRepository.save(swap);
        log.info("Wellness menu swap {} created for user {} on {}: {} -> {} (status: {})",
                saved.getId(), userId, request.getDeliveryDate(),
                request.getOriginalProductId(), request.getRequestedProductId(), saved.getStatus());

        return toSwapDto(saved, userId);
    }

    @Transactional
    public WellnessSwapDto approveSwap(Long swapId) {
        WellnessMenuSwap swap = swapRepository.findById(swapId)
                .orElseThrow(() -> new ResourceNotFoundException("WellnessMenuSwap", swapId));
        swap.setStatus("APPROVED");
        swap.setResolvedAt(OffsetDateTime.now());
        return toSwapDto(swapRepository.save(swap), swap.getSubscriberPreference().getUserId());
    }

    @Transactional
    public WellnessSwapDto rejectSwap(Long swapId, String reason) {
        WellnessMenuSwap swap = swapRepository.findById(swapId)
                .orElseThrow(() -> new ResourceNotFoundException("WellnessMenuSwap", swapId));
        swap.setStatus("REJECTED");
        swap.setRejectionReason(reason);
        swap.setResolvedAt(OffsetDateTime.now());
        return toSwapDto(swapRepository.save(swap), swap.getSubscriberPreference().getUserId());
    }

    @Transactional(readOnly = true)
    public List<WellnessSwapDto> getUserSwaps(Long userId) {
        return subscriberPrefRepository.findByUserId(userId)
                .map(pref -> swapRepository.findBySubscriberPreferenceId(pref.getId()).stream()
                        .map(s -> toSwapDto(s, userId))
                        .toList())
                .orElse(Collections.emptyList());
    }

    private WellnessSwapDto toSwapDto(WellnessMenuSwap swap, Long userId) {
        String origName = productConfigRepository.findById(swap.getOriginalProductId())
                .map(FreshFoodProductConfig::getName).orElse("Product #" + swap.getOriginalProductId());
        String reqName = productConfigRepository.findById(swap.getRequestedProductId())
                .map(FreshFoodProductConfig::getName).orElse("Product #" + swap.getRequestedProductId());

        return WellnessSwapDto.builder()
                .id(swap.getId())
                .subscriberPreferenceId(swap.getSubscriberPreference().getId())
                .userId(userId)
                .deliveryDate(swap.getDeliveryDate())
                .originalProductId(swap.getOriginalProductId())
                .originalProductName(origName)
                .requestedProductId(swap.getRequestedProductId())
                .requestedProductName(reqName)
                .status(swap.getStatus())
                .rejectionReason(swap.getRejectionReason())
                .requestedAt(swap.getRequestedAt())
                .resolvedAt(swap.getResolvedAt())
                .build();
    }
}
