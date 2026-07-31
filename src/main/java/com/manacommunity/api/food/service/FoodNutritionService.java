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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodNutritionService {

    private final FoodNutritionistRepository nutritionistRepo;
    private final FoodNutritionConsultationRepository consultationRepo;
    private final FoodMealPlanRepository mealPlanRepo;
    private final FoodMealPlanItemRepository mealPlanItemRepo;
    private final FoodCalorieLogRepository calorieLogRepo;
    private final FoodWeightLogRepository weightLogRepo;
    private final FoodWaterLogRepository waterLogRepo;

    // ---- Nutritionist ----

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getNutritionists(Long communityId) {
        return nutritionistRepo.findByCommunityIdAndStatus(communityId, "ACTIVE")
                .stream().map(this::toNutritionistResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getNutritionistById(Long id, Long communityId) {
        FoodNutritionist nutritionist = nutritionistRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Nutritionist", id));
        return toNutritionistResponse(nutritionist);
    }

    @Transactional
    public Map<String, Object> registerNutritionist(Map<String, Object> request, AppUser user, Community community) {
        FoodNutritionist nutritionist = FoodNutritionist.builder()
                .user(user)
                .qualification((String) request.get("qualification"))
                .specialization((String) request.get("specialization"))
                .licenseNumber((String) request.get("licenseNumber"))
                .experienceYears(request.get("experienceYears") != null ?
                        Integer.valueOf(request.get("experienceYears").toString()) : null)
                .bio((String) request.get("bio"))
                .consultationFee(request.get("consultationFee") != null ?
                        new BigDecimal(request.get("consultationFee").toString()) : null)
                .profileImageUrl((String) request.get("profileImageUrl"))
                .status(FoodNutritionist.NutritionistStatus.ACTIVE)
                .community(community)
                .build();

        return toNutritionistResponse(nutritionistRepo.save(nutritionist));
    }

    // ---- Consultation ----

    @Transactional
    public Map<String, Object> bookConsultation(Long nutritionistId, Map<String, Object> request,
                                                 AppUser user, Community community) {
        FoodNutritionist nutritionist = nutritionistRepo.findById(nutritionistId)
                .orElseThrow(() -> new ResourceNotFoundException("Nutritionist", nutritionistId));

        FoodNutritionConsultation consultation = FoodNutritionConsultation.builder()
                .nutritionist(nutritionist)
                .user(user)
                .scheduledAt(request.get("scheduledAt") != null ?
                        LocalDateTime.parse((String) request.get("scheduledAt")) : null)
                .durationMinutes(request.get("durationMinutes") != null ?
                        Integer.valueOf(request.get("durationMinutes").toString()) : null)
                .consultationType(request.get("consultationType") != null ?
                        FoodNutritionConsultation.ConsultationType.valueOf((String) request.get("consultationType")) :
                        FoodNutritionConsultation.ConsultationType.VIDEO)
                .status(FoodNutritionConsultation.ConsultationStatus.SCHEDULED)
                .notes((String) request.get("notes"))
                .community(community)
                .build();

        consultation = consultationRepo.save(consultation);

        Map<String, Object> map = new HashMap<>();
        map.put("id", consultation.getId());
        map.put("nutritionistId", consultation.getNutritionist().getId());
        map.put("userId", consultation.getUser().getId());
        map.put("scheduledAt", consultation.getScheduledAt());
        map.put("durationMinutes", consultation.getDurationMinutes());
        map.put("consultationType", consultation.getConsultationType() != null ?
                consultation.getConsultationType().name() : null);
        map.put("status", consultation.getStatus() != null ? consultation.getStatus().name() : null);
        map.put("notes", consultation.getNotes());
        map.put("communityId", consultation.getCommunity() != null ? consultation.getCommunity().getId() : null);
        map.put("createdAt", consultation.getCreatedAt());
        map.put("updatedAt", consultation.getUpdatedAt());
        return map;
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getMyConsultations(Long userId, Long communityId, Pageable pageable) {
        return consultationRepo.findByUserIdAndCommunityId(userId, communityId, pageable)
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getId());
                    map.put("nutritionistId", c.getNutritionist() != null ? c.getNutritionist().getId() : null);
                    map.put("userId", c.getUser() != null ? c.getUser().getId() : null);
                    map.put("scheduledAt", c.getScheduledAt());
                    map.put("durationMinutes", c.getDurationMinutes());
                    map.put("consultationType", c.getConsultationType() != null ?
                            c.getConsultationType().name() : null);
                    map.put("status", c.getStatus() != null ? c.getStatus().name() : null);
                    map.put("notes", c.getNotes());
                    map.put("followUpDate", c.getFollowUpDate());
                    map.put("communityId", c.getCommunity() != null ? c.getCommunity().getId() : null);
                    map.put("createdAt", c.getCreatedAt());
                    map.put("updatedAt", c.getUpdatedAt());
                    return map;
                });
    }

    // ---- Meal Plan ----

    @Transactional
    public Map<String, Object> createMealPlan(Map<String, Object> request, AppUser user, Community community) {
        FoodMealPlan plan = FoodMealPlan.builder()
                .user(user)
                .name((String) request.get("name"))
                .description((String) request.get("description"))
                .startDate(request.get("startDate") != null ?
                        LocalDate.parse((String) request.get("startDate")) : null)
                .endDate(request.get("endDate") != null ?
                        LocalDate.parse((String) request.get("endDate")) : null)
                .goal((String) request.get("goal"))
                .dailyCalories(request.get("dailyCalories") != null ?
                        Integer.valueOf(request.get("dailyCalories").toString()) : null)
                .dailyProtein(request.get("dailyProtein") != null ?
                        new BigDecimal(request.get("dailyProtein").toString()) : null)
                .status(FoodMealPlan.MealPlanStatus.ACTIVE)
                .community(community)
                .build();

        return toMealPlanResponse(mealPlanRepo.save(plan));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyMealPlans(Long userId, Long communityId) {
        return mealPlanRepo.findByUserIdAndCommunityId(userId, communityId)
                .stream().map(this::toMealPlanResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMealPlanById(Long id) {
        FoodMealPlan plan = mealPlanRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MealPlan", id));

        Map<String, Object> response = toMealPlanResponse(plan);
        List<FoodMealPlanItem> items = mealPlanItemRepo.findByPlanId(id);
        response.put("items", items.stream().map(item -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", item.getId());
            m.put("dayOfWeek", item.getDayOfWeek());
            m.put("mealType", item.getMealType());
            m.put("recipeId", item.getRecipeId());
            m.put("itemName", item.getItemName());
            m.put("calories", item.getCalories());
            m.put("protein", item.getProtein());
            m.put("carbs", item.getCarbs());
            m.put("fat", item.getFat());
            m.put("portionSize", item.getPortionSize());
            m.put("notes", item.getNotes());
            return m;
        }).collect(Collectors.toList()));

        return response;
    }

    // ---- Calorie Log ----

    @Transactional
    public Map<String, Object> logCalories(Map<String, Object> request, AppUser user, Community community) {
        FoodCalorieLog log = FoodCalorieLog.builder()
                .user(user)
                .date(request.get("date") != null ?
                        LocalDate.parse((String) request.get("date")) : LocalDate.now())
                .mealType((String) request.get("mealType"))
                .itemName((String) request.get("itemName"))
                .calories(request.get("calories") != null ?
                        Integer.valueOf(request.get("calories").toString()) : null)
                .protein(request.get("protein") != null ?
                        new BigDecimal(request.get("protein").toString()) : null)
                .carbs(request.get("carbs") != null ?
                        new BigDecimal(request.get("carbs").toString()) : null)
                .fat(request.get("fat") != null ?
                        new BigDecimal(request.get("fat").toString()) : null)
                .fiber(request.get("fiber") != null ?
                        new BigDecimal(request.get("fiber").toString()) : null)
                .quantity(request.get("quantity") != null ?
                        new BigDecimal(request.get("quantity").toString()) : null)
                .unit((String) request.get("unit"))
                .source(request.get("source") != null ?
                        FoodCalorieLog.CalorieSource.valueOf((String) request.get("source")) :
                        FoodCalorieLog.CalorieSource.MANUAL)
                .referenceId(request.get("referenceId") != null ?
                        Long.valueOf(request.get("referenceId").toString()) : null)
                .community(community)
                .build();

        log = calorieLogRepo.save(log);

        Map<String, Object> map = new HashMap<>();
        map.put("id", log.getId());
        map.put("userId", log.getUser().getId());
        map.put("date", log.getDate());
        map.put("mealType", log.getMealType());
        map.put("itemName", log.getItemName());
        map.put("calories", log.getCalories());
        map.put("protein", log.getProtein());
        map.put("carbs", log.getCarbs());
        map.put("fat", log.getFat());
        map.put("fiber", log.getFiber());
        map.put("quantity", log.getQuantity());
        map.put("unit", log.getUnit());
        map.put("source", log.getSource() != null ? log.getSource().name() : null);
        map.put("createdAt", log.getCreatedAt());
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCalorieLogs(Long userId, LocalDate date) {
        return calorieLogRepo.findByUserIdAndDate(userId, date)
                .stream().map(log -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", log.getId());
                    map.put("date", log.getDate());
                    map.put("mealType", log.getMealType());
                    map.put("itemName", log.getItemName());
                    map.put("calories", log.getCalories());
                    map.put("protein", log.getProtein());
                    map.put("carbs", log.getCarbs());
                    map.put("fat", log.getFat());
                    map.put("fiber", log.getFiber());
                    map.put("quantity", log.getQuantity());
                    map.put("unit", log.getUnit());
                    map.put("source", log.getSource() != null ? log.getSource().name() : null);
                    map.put("createdAt", log.getCreatedAt());
                    return map;
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDailyNutrition(Long userId, LocalDate date) {
        List<FoodCalorieLog> logs = calorieLogRepo.findByUserIdAndDate(userId, date);

        int totalCalories = logs.stream()
                .filter(l -> l.getCalories() != null)
                .mapToInt(FoodCalorieLog::getCalories).sum();
        BigDecimal totalProtein = logs.stream()
                .filter(l -> l.getProtein() != null)
                .map(FoodCalorieLog::getProtein)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCarbs = logs.stream()
                .filter(l -> l.getCarbs() != null)
                .map(FoodCalorieLog::getCarbs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFat = logs.stream()
                .filter(l -> l.getFat() != null)
                .map(FoodCalorieLog::getFat)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFiber = logs.stream()
                .filter(l -> l.getFiber() != null)
                .map(FoodCalorieLog::getFiber)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("date", date);
        map.put("totalCalories", totalCalories);
        map.put("totalProtein", totalProtein);
        map.put("totalCarbs", totalCarbs);
        map.put("totalFat", totalFat);
        map.put("totalFiber", totalFiber);
        map.put("mealCount", logs.size());
        return map;
    }

    // ---- Weight Log ----

    @Transactional
    public Map<String, Object> logWeight(Map<String, Object> request, AppUser user, Community community) {
        FoodWeightLog log = FoodWeightLog.builder()
                .user(user)
                .date(request.get("date") != null ?
                        LocalDate.parse((String) request.get("date")) : LocalDate.now())
                .weight(request.get("weight") != null ?
                        new BigDecimal(request.get("weight").toString()) : null)
                .unit(request.get("unit") != null ?
                        FoodWeightLog.WeightUnit.valueOf((String) request.get("unit")) :
                        FoodWeightLog.WeightUnit.KG)
                .bodyFatPct(request.get("bodyFatPct") != null ?
                        new BigDecimal(request.get("bodyFatPct").toString()) : null)
                .muscleMass(request.get("muscleMass") != null ?
                        new BigDecimal(request.get("muscleMass").toString()) : null)
                .notes((String) request.get("notes"))
                .community(community)
                .build();

        log = weightLogRepo.save(log);

        Map<String, Object> map = new HashMap<>();
        map.put("id", log.getId());
        map.put("userId", log.getUser().getId());
        map.put("date", log.getDate());
        map.put("weight", log.getWeight());
        map.put("unit", log.getUnit() != null ? log.getUnit().name() : null);
        map.put("bodyFatPct", log.getBodyFatPct());
        map.put("muscleMass", log.getMuscleMass());
        map.put("notes", log.getNotes());
        map.put("createdAt", log.getCreatedAt());
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getWeightHistory(Long userId) {
        return weightLogRepo.findByUserIdOrderByDateDesc(userId)
                .stream().map(log -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", log.getId());
                    map.put("date", log.getDate());
                    map.put("weight", log.getWeight());
                    map.put("unit", log.getUnit() != null ? log.getUnit().name() : null);
                    map.put("bodyFatPct", log.getBodyFatPct());
                    map.put("muscleMass", log.getMuscleMass());
                    map.put("notes", log.getNotes());
                    map.put("createdAt", log.getCreatedAt());
                    return map;
                }).collect(Collectors.toList());
    }

    // ---- Water Log ----

    @Transactional
    public Map<String, Object> logWater(Long userId, LocalDate date, Integer intakeMl, Community community) {
        AppUser user = new AppUser();
        user.setId(userId);

        FoodWaterLog log = FoodWaterLog.builder()
                .user(user)
                .date(date != null ? date : LocalDate.now())
                .intakeMl(intakeMl)
                .community(community)
                .build();

        log = waterLogRepo.save(log);

        Map<String, Object> map = new HashMap<>();
        map.put("id", log.getId());
        map.put("userId", userId);
        map.put("date", log.getDate());
        map.put("intakeMl", log.getIntakeMl());
        map.put("goalMl", log.getGoalMl());
        map.put("createdAt", log.getCreatedAt());
        return map;
    }

    // ---- Private mappers ----

    private Map<String, Object> toNutritionistResponse(FoodNutritionist n) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", n.getId());
        map.put("userId", n.getUser() != null ? n.getUser().getId() : null);
        map.put("qualification", n.getQualification());
        map.put("specialization", n.getSpecialization());
        map.put("licenseNumber", n.getLicenseNumber());
        map.put("experienceYears", n.getExperienceYears());
        map.put("bio", n.getBio());
        map.put("consultationFee", n.getConsultationFee());
        map.put("rating", n.getRating());
        map.put("status", n.getStatus() != null ? n.getStatus().name() : null);
        map.put("profileImageUrl", n.getProfileImageUrl());
        map.put("communityId", n.getCommunity() != null ? n.getCommunity().getId() : null);
        map.put("createdAt", n.getCreatedAt());
        map.put("updatedAt", n.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toMealPlanResponse(FoodMealPlan p) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.getId());
        map.put("userId", p.getUser() != null ? p.getUser().getId() : null);
        map.put("name", p.getName());
        map.put("description", p.getDescription());
        map.put("startDate", p.getStartDate());
        map.put("endDate", p.getEndDate());
        map.put("goal", p.getGoal());
        map.put("dailyCalories", p.getDailyCalories());
        map.put("dailyProtein", p.getDailyProtein());
        map.put("status", p.getStatus() != null ? p.getStatus().name() : null);
        map.put("communityId", p.getCommunity() != null ? p.getCommunity().getId() : null);
        map.put("createdAt", p.getCreatedAt());
        map.put("updatedAt", p.getUpdatedAt());
        return map;
    }
}
