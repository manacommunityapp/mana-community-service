package com.manacommunity.api.wellness.service;

import com.manacommunity.api.wellness.dto.*;
import com.manacommunity.api.wellness.entity.*;
import com.manacommunity.api.wellness.enums.WellnessEnums.*;
import com.manacommunity.api.wellness.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WellnessService {

    private static final Map<String, BigDecimal> JUNK_FOOD_SCORES = Map.ofEntries(
        Map.entry("pizza", new BigDecimal("0.90")), Map.entry("burger", new BigDecimal("0.90")),
        Map.entry("fries", new BigDecimal("0.85")), Map.entry("french fries", new BigDecimal("0.90")),
        Map.entry("chips", new BigDecimal("0.80")), Map.entry("donut", new BigDecimal("0.90")),
        Map.entry("nuggets", new BigDecimal("0.85")), Map.entry("hotdog", new BigDecimal("0.85")),
        Map.entry("soda", new BigDecimal("0.75")), Map.entry("cola", new BigDecimal("0.75")),
        Map.entry("candy", new BigDecimal("0.85")), Map.entry("cake", new BigDecimal("0.65")),
        Map.entry("cookie", new BigDecimal("0.65")), Map.entry("ice cream", new BigDecimal("0.70")),
        Map.entry("deep fried", new BigDecimal("0.80")),
        Map.entry("vada pav", new BigDecimal("0.80")), Map.entry("pav bhaji", new BigDecimal("0.75")),
        Map.entry("samosa", new BigDecimal("0.75")), Map.entry("kachori", new BigDecimal("0.80")),
        Map.entry("jalebi", new BigDecimal("0.85")), Map.entry("gulab jamun", new BigDecimal("0.85")),
        Map.entry("namkeen", new BigDecimal("0.70"))
    );
    private static final BigDecimal JUNK_THRESHOLD = new BigDecimal("0.60");

    private final WellnessProfileRepository profileRepository;
    private final MealLogRepository mealLogRepository;
    private final WaterLogRepository waterLogRepository;
    private final FoodCatalogRepository foodCatalogRepository;
    private final WellnessChallengeRepository challengeRepository;
    private final ChallengeParticipantRepository participantRepository;
    private final WellnessProviderRepository providerRepository;
    private final AccountabilityPartnerRepository partnerRepository;
    private final AccountabilityAlertRepository alertRepository;

    // ── Profile ──

    public WellnessProfileDto getProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .map(this::toProfileDto)
                .orElse(WellnessProfileDto.builder().userId(userId).build());
    }

    @Transactional
    public WellnessProfileDto saveProfile(Long userId, WellnessProfileDto dto) {
        WellnessProfile profile = profileRepository.findByUserId(userId)
                .orElse(WellnessProfile.builder().userId(userId).build());
        profile.setHeightCm(dto.getHeightCm());
        profile.setWeightKg(dto.getWeightKg());
        profile.setTargetWeightKg(dto.getTargetWeightKg());
        profile.setAge(dto.getAge());
        profile.setGender(dto.getGender());
        profile.setActivityLevel(dto.getActivityLevel());
        profile.setGoalType(dto.getGoalType());
        profile.setCoachingStyle(dto.getCoachingStyle());
        profile.setDailyCalorieTarget(dto.getDailyCalorieTarget());
        profile.setDailyProteinTarget(dto.getDailyProteinTarget());
        profile.setDailyCarbsTarget(dto.getDailyCarbsTarget());
        profile.setDailyFatTarget(dto.getDailyFatTarget());
        profile.setDailyWaterTargetMl(dto.getDailyWaterTargetMl());
        profile.setSharingPreference(dto.getSharingPreference());
        profile.setDietaryRestrictions(dto.getDietaryRestrictions());
        profile.setAllergies(dto.getAllergies());
        profile.setHealthConditions(dto.getHealthConditions());
        return toProfileDto(profileRepository.save(profile));
    }

    // ── Meals ──

    public List<MealLogDto> getMealsForDate(Long userId, LocalDate date) {
        return mealLogRepository.findByUserIdAndMealDateOrderByMealTimeDesc(userId, date)
                .stream().map(this::toMealLogDto).collect(Collectors.toList());
    }

    public List<MealLogDto> getMealsForRange(Long userId, LocalDate start, LocalDate end) {
        return mealLogRepository.findByUserIdAndMealDateBetweenOrderByMealDateDescMealTimeDesc(userId, start, end)
                .stream().map(this::toMealLogDto).collect(Collectors.toList());
    }

    @Transactional
    public MealLogDto logMeal(Long userId, MealLogDto dto) {
        MealLog mealLog = MealLog.builder()
                .userId(userId)
                .mealType(dto.getMealType())
                .mealDate(dto.getMealDate() != null ? dto.getMealDate() : LocalDate.now())
                .mealTime(dto.getMealTime() != null ? dto.getMealTime() : LocalTime.now())
                .sourceType(dto.getSourceType())
                .originalInput(dto.getOriginalInput())
                .photoUrl(dto.getPhotoUrl())
                .totalCalories(dto.getTotalCalories())
                .totalProtein(dto.getTotalProtein())
                .totalCarbs(dto.getTotalCarbs())
                .totalFat(dto.getTotalFat())
                .totalFiber(dto.getTotalFiber())
                .aiConfidence(dto.getAiConfidence())
                .status(dto.getStatus() != null ? dto.getStatus() : MealLogStatus.CONFIRMED)
                .notes(dto.getNotes())
                .build();

        BigDecimal junkScore = detectJunkFoodScore(dto.getItems(), dto.getOriginalInput());
        mealLog.setJunkFoodScore(junkScore);
        mealLog.setIsJunkFood(junkScore.compareTo(JUNK_THRESHOLD) >= 0);

        if (dto.getItems() != null) {
            for (MealItemDto itemDto : dto.getItems()) {
                MealItem item = MealItem.builder()
                        .mealLog(mealLog)
                        .foodName(itemDto.getFoodName())
                        .foodNameLocal(itemDto.getFoodNameLocal())
                        .quantity(itemDto.getQuantity())
                        .unit(itemDto.getUnit())
                        .calories(itemDto.getCalories())
                        .protein(itemDto.getProtein())
                        .carbs(itemDto.getCarbs())
                        .fat(itemDto.getFat())
                        .fiber(itemDto.getFiber())
                        .foodCatalogId(itemDto.getFoodCatalogId())
                        .build();
                mealLog.getItems().add(item);
            }
        }

        MealLogDto saved = toMealLogDto(mealLogRepository.save(mealLog));
        if (Boolean.TRUE.equals(mealLog.getIsJunkFood())) {
            createJunkFoodAlert(userId, saved);
        }
        return saved;
    }

    @Transactional
    public void deleteMealLog(Long userId, Long mealLogId) {
        mealLogRepository.findById(mealLogId)
                .filter(m -> m.getUserId().equals(userId))
                .ifPresent(mealLogRepository::delete);
    }

    // ── Daily Summary ──

    public DailyNutritionSummaryDto getDailySummary(Long userId, LocalDate date) {
        List<MealLog> meals = mealLogRepository.findByUserIdAndMealDateOrderByMealTimeDesc(userId, date);
        WellnessProfile profile = profileRepository.findByUserId(userId).orElse(null);
        Integer waterTotal = waterLogRepository.getTotalWaterForDate(userId, date);

        int totalCalories = meals.stream().mapToInt(m -> m.getTotalCalories() != null ? m.getTotalCalories() : 0).sum();
        BigDecimal totalProtein = meals.stream().map(m -> m.getTotalProtein() != null ? m.getTotalProtein() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCarbs = meals.stream().map(m -> m.getTotalCarbs() != null ? m.getTotalCarbs() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFat = meals.stream().map(m -> m.getTotalFat() != null ? m.getTotalFat() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFiber = meals.stream().map(m -> m.getTotalFiber() != null ? m.getTotalFiber() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);

        return DailyNutritionSummaryDto.builder()
                .date(date)
                .totalCalories(totalCalories)
                .calorieTarget(profile != null ? profile.getDailyCalorieTarget() : 2000)
                .totalProtein(totalProtein)
                .totalCarbs(totalCarbs)
                .totalFat(totalFat)
                .totalFiber(totalFiber)
                .totalWaterMl(waterTotal)
                .waterTargetMl(profile != null && profile.getDailyWaterTargetMl() != null ? profile.getDailyWaterTargetMl() : 2500)
                .mealCount(meals.size())
                .meals(meals.stream().map(this::toMealLogDto).collect(Collectors.toList()))
                .build();
    }

    // ── Water ──

    public List<WaterLogDto> getWaterLogsForDate(Long userId, LocalDate date) {
        return waterLogRepository.findByUserIdAndLogDateOrderByLogTimeDesc(userId, date)
                .stream().map(this::toWaterLogDto).collect(Collectors.toList());
    }

    @Transactional
    public WaterLogDto logWater(Long userId, WaterLogDto dto) {
        WaterLog log = WaterLog.builder()
                .userId(userId)
                .logDate(dto.getLogDate() != null ? dto.getLogDate() : LocalDate.now())
                .logTime(dto.getLogTime() != null ? dto.getLogTime() : LocalTime.now())
                .amountMl(dto.getAmountMl())
                .notes(dto.getNotes())
                .build();
        return toWaterLogDto(waterLogRepository.save(log));
    }

    // ── Food Catalog ──

    public List<FoodCatalog> searchFood(String query) {
        return foodCatalogRepository.searchByName(query);
    }

    // ── Challenges ──

    public List<WellnessChallengeDto> getChallenges(ChallengeStatus status) {
        List<WellnessChallenge> challenges = status != null
                ? challengeRepository.findByStatus(status)
                : challengeRepository.findAll();
        return challenges.stream().map(this::toChallengeDto).collect(Collectors.toList());
    }

    @Transactional
    public ChallengeParticipantDto joinChallenge(Long userId, Long challengeId) {
        if (participantRepository.findByChallengeIdAndUserId(challengeId, userId).isPresent()) {
            throw new IllegalStateException("Already joined this challenge");
        }
        ChallengeParticipant participant = ChallengeParticipant.builder()
                .challenge(challengeRepository.findById(challengeId).orElseThrow(() -> new IllegalArgumentException("Challenge not found")))
                .userId(userId)
                .currentProgress(0)
                .joinedDate(LocalDate.now())
                .completed(false)
                .badgeEarned(false)
                .build();
        return toParticipantDto(participantRepository.save(participant));
    }

    @Transactional
    public ChallengeParticipantDto updateProgress(Long userId, Long challengeId, Integer progress) {
        ChallengeParticipant participant = participantRepository.findByChallengeIdAndUserId(challengeId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Not a participant"));
        participant.setCurrentProgress(progress);
        WellnessChallenge challenge = participant.getChallenge();
        if (challenge.getTargetValue() != null && progress >= challenge.getTargetValue()) {
            participant.setCompleted(true);
            participant.setCompletedDate(LocalDate.now());
            participant.setBadgeEarned(true);
        }
        return toParticipantDto(participantRepository.save(participant));
    }

    // ── Providers ──

    public List<WellnessProviderDto> getProviders(WellnessProviderType type) {
        List<WellnessProvider> providers = type != null
                ? providerRepository.findByProviderTypeAndActiveTrue(type)
                : providerRepository.findByActiveTrue();
        return providers.stream().map(this::toProviderDto).collect(Collectors.toList());
    }

    // ── Mappers ──

    private WellnessProfileDto toProfileDto(WellnessProfile p) {
        return WellnessProfileDto.builder()
                .id(p.getId()).userId(p.getUserId())
                .heightCm(p.getHeightCm()).weightKg(p.getWeightKg()).targetWeightKg(p.getTargetWeightKg())
                .age(p.getAge()).gender(p.getGender())
                .activityLevel(p.getActivityLevel()).goalType(p.getGoalType()).coachingStyle(p.getCoachingStyle())
                .dailyCalorieTarget(p.getDailyCalorieTarget()).dailyProteinTarget(p.getDailyProteinTarget())
                .dailyCarbsTarget(p.getDailyCarbsTarget()).dailyFatTarget(p.getDailyFatTarget())
                .dailyWaterTargetMl(p.getDailyWaterTargetMl())
                .sharingPreference(p.getSharingPreference())
                .dietaryRestrictions(p.getDietaryRestrictions()).allergies(p.getAllergies()).healthConditions(p.getHealthConditions())
                .build();
    }

    private MealLogDto toMealLogDto(MealLog m) {
        return MealLogDto.builder()
                .id(m.getId()).userId(m.getUserId())
                .mealType(m.getMealType()).mealDate(m.getMealDate()).mealTime(m.getMealTime())
                .sourceType(m.getSourceType()).originalInput(m.getOriginalInput()).photoUrl(m.getPhotoUrl())
                .totalCalories(m.getTotalCalories()).totalProtein(m.getTotalProtein())
                .totalCarbs(m.getTotalCarbs()).totalFat(m.getTotalFat()).totalFiber(m.getTotalFiber())
                .aiConfidence(m.getAiConfidence()).junkFoodScore(m.getJunkFoodScore()).isJunkFood(m.getIsJunkFood())
                .status(m.getStatus()).notes(m.getNotes())
                .items(m.getItems() != null ? m.getItems().stream().map(this::toMealItemDto).collect(Collectors.toList()) : List.of())
                .build();
    }

    private MealItemDto toMealItemDto(MealItem i) {
        return MealItemDto.builder()
                .id(i.getId()).foodName(i.getFoodName()).foodNameLocal(i.getFoodNameLocal())
                .quantity(i.getQuantity()).unit(i.getUnit())
                .calories(i.getCalories()).protein(i.getProtein()).carbs(i.getCarbs()).fat(i.getFat()).fiber(i.getFiber())
                .foodCatalogId(i.getFoodCatalogId())
                .build();
    }

    private WaterLogDto toWaterLogDto(WaterLog w) {
        return WaterLogDto.builder()
                .id(w.getId()).userId(w.getUserId())
                .logDate(w.getLogDate()).logTime(w.getLogTime())
                .amountMl(w.getAmountMl()).notes(w.getNotes())
                .build();
    }

    private WellnessChallengeDto toChallengeDto(WellnessChallenge c) {
        return WellnessChallengeDto.builder()
                .id(c.getId()).title(c.getTitle()).description(c.getDescription())
                .challengeType(c.getChallengeType()).status(c.getStatus())
                .startDate(c.getStartDate()).endDate(c.getEndDate())
                .targetValue(c.getTargetValue()).targetUnit(c.getTargetUnit())
                .maxParticipants(c.getMaxParticipants()).communityId(c.getCommunityId())
                .createdByUserId(c.getCreatedByUserId())
                .badgeName(c.getBadgeName()).badgeIcon(c.getBadgeIcon())
                .participantCount(c.getParticipants() != null ? c.getParticipants().size() : 0)
                .build();
    }

    private ChallengeParticipantDto toParticipantDto(ChallengeParticipant p) {
        return ChallengeParticipantDto.builder()
                .id(p.getId()).userId(p.getUserId())
                .currentProgress(p.getCurrentProgress()).joinedDate(p.getJoinedDate())
                .completed(p.getCompleted()).completedDate(p.getCompletedDate()).badgeEarned(p.getBadgeEarned())
                .build();
    }

    private WellnessProviderDto toProviderDto(WellnessProvider p) {
        return WellnessProviderDto.builder()
                .id(p.getId()).name(p.getName()).providerType(p.getProviderType())
                .specialty(p.getSpecialty()).description(p.getDescription())
                .experienceYears(p.getExperienceYears()).rating(p.getRating()).totalReviews(p.getTotalReviews())
                .consultationFee(p.getConsultationFee())
                .availableOnline(p.getAvailableOnline()).availableInPerson(p.getAvailableInPerson())
                .location(p.getLocation()).phone(p.getPhone()).email(p.getEmail()).photoUrl(p.getPhotoUrl())
                .communityId(p.getCommunityId()).verified(p.getVerified()).active(p.getActive())
                .build();
    }

    // ── Junk Food Detection ──

    private BigDecimal detectJunkFoodScore(List<MealItemDto> items, String originalInput) {
        String searchText = (originalInput != null ? originalInput.toLowerCase() : "") + " " +
                (items != null ? items.stream().map(i -> i.getFoodName().toLowerCase()).collect(Collectors.joining(" ")) : "");
        return JUNK_FOOD_SCORES.entrySet().stream()
                .filter(e -> searchText.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private void createJunkFoodAlert(Long userId, MealLogDto meal) {
        List<AccountabilityPartner> activePartners = partnerRepository.findByUserIdAndActiveTrue(userId).stream()
                .filter(p -> p.getConsentStatus() == ConsentStatus.ACCEPTED)
                .toList();
        if (activePartners.isEmpty()) return;

        String foodNames = meal.getItems() != null
                ? meal.getItems().stream().map(MealItemDto::getFoodName).collect(Collectors.joining(", "))
                : "unknown foods";
        int scorePercent = meal.getJunkFoodScore() != null
                ? meal.getJunkFoodScore().multiply(new BigDecimal("100")).intValue() : 0;

        for (AccountabilityPartner partner : activePartners) {
            alertRepository.save(AccountabilityAlert.builder()
                    .userId(userId)
                    .partnerUserId(partner.getPartnerUserId())
                    .alertType(AlertType.JUNK_FOOD)
                    .triggeredAt(LocalDateTime.now())
                    .message(String.format("Junk food detected: %s (score %d%%)", foodNames, scorePercent))
                    .isRead(false)
                    .build());
        }
    }

    // ── Alerts ──

    public List<AccountabilityAlertDto> getAlerts(Long userId) {
        return alertRepository.findByUserIdOrderByTriggeredAtDesc(userId)
                .stream().map(this::toAlertDto).collect(Collectors.toList());
    }

    @Transactional
    public void markAlertRead(Long userId, Long alertId) {
        alertRepository.findById(alertId)
                .filter(a -> a.getUserId().equals(userId))
                .ifPresent(a -> { a.setIsRead(true); alertRepository.save(a); });
    }

    private AccountabilityAlertDto toAlertDto(AccountabilityAlert a) {
        return AccountabilityAlertDto.builder()
                .id(a.getId()).alertType(a.getAlertType()).partnerUserId(a.getPartnerUserId())
                .triggeredAt(a.getTriggeredAt()).message(a.getMessage())
                .isRead(a.getIsRead()).dataJson(a.getDataJson())
                .build();
    }
}
