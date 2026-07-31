package com.manacommunity.api.food.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.food.entity.FoodResidentAllergy;
import com.manacommunity.api.food.entity.FoodResidentFavorite;
import com.manacommunity.api.food.entity.FoodResidentGoal;
import com.manacommunity.api.food.entity.FoodResidentProfile;
import com.manacommunity.api.food.repository.FoodResidentAllergyRepository;
import com.manacommunity.api.food.repository.FoodResidentFavoriteRepository;
import com.manacommunity.api.food.repository.FoodResidentGoalRepository;
import com.manacommunity.api.food.repository.FoodResidentProfileRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodResidentProfileService {

    private final FoodResidentProfileRepository profileRepo;
    private final FoodResidentAllergyRepository allergyRepo;
    private final FoodResidentFavoriteRepository favoriteRepo;
    private final FoodResidentGoalRepository goalRepo;

    @Transactional(readOnly = true)
    public Map<String, Object> getProfile(Long userId, Long communityId) {
        FoodResidentProfile profile = profileRepo.findByUserIdAndCommunityId(userId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("ResidentProfile", "userId", userId.toString()));
        return toResponse(profile);
    }

    @Transactional
    public Map<String, Object> createOrUpdateProfile(Map<String, Object> request, AppUser user, Community community) {
        FoodResidentProfile profile = profileRepo.findByUserIdAndCommunityId(user.getId(), community.getId())
                .orElse(FoodResidentProfile.builder()
                        .user(user)
                        .community(community)
                        .build());

        if (request.containsKey("dietType")) {
            profile.setDietType(FoodResidentProfile.DietType.valueOf((String) request.get("dietType")));
        }
        if (request.containsKey("calorieGoal")) {
            profile.setCalorieGoal((Integer) request.get("calorieGoal"));
        }
        if (request.containsKey("proteinGoal")) {
            profile.setProteinGoal(new BigDecimal(request.get("proteinGoal").toString()));
        }
        if (request.containsKey("weightGoal")) {
            profile.setWeightGoal(new BigDecimal(request.get("weightGoal").toString()));
        }
        if (request.containsKey("healthGoal")) {
            profile.setHealthGoal((String) request.get("healthGoal"));
        }
        if (request.containsKey("fitnessGoal")) {
            profile.setFitnessGoal((String) request.get("fitnessGoal"));
        }
        if (request.containsKey("waterIntakeGoal")) {
            profile.setWaterIntakeGoal((Integer) request.get("waterIntakeGoal"));
        }
        if (request.containsKey("coffeeLimit")) {
            profile.setCoffeeLimit((Integer) request.get("coffeeLimit"));
        }
        if (request.containsKey("bmi")) {
            profile.setBmi(new BigDecimal(request.get("bmi").toString()));
        }

        FoodResidentProfile saved = profileRepo.save(profile);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllergies(Long profileId) {
        List<FoodResidentAllergy> allergies = allergyRepo.findByProfileId(profileId);
        return allergies.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("profileId", a.getProfile().getId());
            map.put("allergyName", a.getAllergyName());
            map.put("severity", a.getSeverity() != null ? a.getSeverity().name() : null);
            map.put("notes", a.getNotes());
            map.put("createdAt", a.getCreatedAt());
            map.put("updatedAt", a.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> addAllergy(Long profileId, Map<String, Object> request) {
        FoodResidentProfile profile = profileRepo.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("ResidentProfile", profileId));

        FoodResidentAllergy allergy = FoodResidentAllergy.builder()
                .profile(profile)
                .allergyName((String) request.get("allergyName"))
                .severity(request.containsKey("severity")
                        ? FoodResidentAllergy.Severity.valueOf((String) request.get("severity"))
                        : null)
                .notes((String) request.get("notes"))
                .build();

        FoodResidentAllergy saved = allergyRepo.save(allergy);

        Map<String, Object> map = new HashMap<>();
        map.put("id", saved.getId());
        map.put("profileId", saved.getProfile().getId());
        map.put("allergyName", saved.getAllergyName());
        map.put("severity", saved.getSeverity() != null ? saved.getSeverity().name() : null);
        map.put("notes", saved.getNotes());
        map.put("createdAt", saved.getCreatedAt());
        map.put("updatedAt", saved.getUpdatedAt());
        return map;
    }

    @Transactional
    public void removeAllergy(Long allergyId) {
        if (!allergyRepo.existsById(allergyId)) {
            throw new ResourceNotFoundException("ResidentAllergy", allergyId);
        }
        allergyRepo.deleteById(allergyId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFavorites(Long profileId, String type) {
        List<FoodResidentFavorite> favorites;
        if (type != null && !type.isBlank()) {
            FoodResidentFavorite.FavoriteType favoriteType = FoodResidentFavorite.FavoriteType.valueOf(type);
            favorites = favoriteRepo.findByProfileIdAndFavoriteType(profileId, favoriteType);
        } else {
            favorites = favoriteRepo.findByProfileId(profileId);
        }
        return favorites.stream().map(f -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", f.getId());
            map.put("profileId", f.getProfile().getId());
            map.put("favoriteType", f.getFavoriteType().name());
            map.put("referenceId", f.getReferenceId());
            map.put("createdAt", f.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> addFavorite(Long profileId, String type, Long referenceId) {
        FoodResidentProfile profile = profileRepo.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("ResidentProfile", profileId));

        FoodResidentFavorite favorite = FoodResidentFavorite.builder()
                .profile(profile)
                .favoriteType(FoodResidentFavorite.FavoriteType.valueOf(type))
                .referenceId(referenceId)
                .build();

        FoodResidentFavorite saved = favoriteRepo.save(favorite);

        Map<String, Object> map = new HashMap<>();
        map.put("id", saved.getId());
        map.put("profileId", saved.getProfile().getId());
        map.put("favoriteType", saved.getFavoriteType().name());
        map.put("referenceId", saved.getReferenceId());
        map.put("createdAt", saved.getCreatedAt());
        return map;
    }

    @Transactional
    public void removeFavorite(Long favoriteId) {
        if (!favoriteRepo.existsById(favoriteId)) {
            throw new ResourceNotFoundException("ResidentFavorite", favoriteId);
        }
        favoriteRepo.deleteById(favoriteId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getGoals(Long profileId) {
        List<FoodResidentGoal> goals = goalRepo.findByProfileIdAndStatus(profileId, FoodResidentGoal.GoalStatus.ACTIVE);
        return goals.stream().map(g -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", g.getId());
            map.put("profileId", g.getProfile().getId());
            map.put("goalType", g.getGoalType());
            map.put("targetValue", g.getTargetValue());
            map.put("currentValue", g.getCurrentValue());
            map.put("unit", g.getUnit());
            map.put("startDate", g.getStartDate());
            map.put("targetDate", g.getTargetDate());
            map.put("status", g.getStatus().name());
            map.put("createdAt", g.getCreatedAt());
            map.put("updatedAt", g.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> toResponse(FoodResidentProfile profile) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", profile.getId());
        map.put("userId", profile.getUser().getId());
        map.put("dietType", profile.getDietType() != null ? profile.getDietType().name() : null);
        map.put("calorieGoal", profile.getCalorieGoal());
        map.put("proteinGoal", profile.getProteinGoal());
        map.put("weightGoal", profile.getWeightGoal());
        map.put("healthGoal", profile.getHealthGoal());
        map.put("fitnessGoal", profile.getFitnessGoal());
        map.put("waterIntakeGoal", profile.getWaterIntakeGoal());
        map.put("coffeeLimit", profile.getCoffeeLimit());
        map.put("dailyNutritionScore", profile.getDailyNutritionScore());
        map.put("aiLifestyleScore", profile.getAiLifestyleScore());
        map.put("bmi", profile.getBmi());
        map.put("communityId", profile.getCommunity().getId());
        map.put("createdAt", profile.getCreatedAt());
        map.put("updatedAt", profile.getUpdatedAt());
        return map;
    }
}
