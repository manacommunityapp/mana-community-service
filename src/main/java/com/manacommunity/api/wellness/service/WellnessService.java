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
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WellnessService {

    private final WellnessProfileRepository profileRepository;
    private final MealLogRepository mealLogRepository;
    private final WaterLogRepository waterLogRepository;
    private final FoodCatalogRepository foodCatalogRepository;
    private final WellnessChallengeRepository challengeRepository;
    private final ChallengeParticipantRepository participantRepository;
    private final WellnessProviderRepository providerRepository;
    private final AccountabilityPartnerRepository partnerRepository;

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

        return toMealLogDto(mealLogRepository.save(mealLog));
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
                .aiConfidence(m.getAiConfidence()).status(m.getStatus()).notes(m.getNotes())
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
}
