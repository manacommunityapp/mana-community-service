package com.manacommunity.api.wellness.dto;

import com.manacommunity.api.wellness.enums.WellnessEnums.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessProfileDto {
    private Long id;
    private Long userId;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private BigDecimal targetWeightKg;
    private Integer age;
    private String gender;
    private ActivityLevel activityLevel;
    private GoalType goalType;
    private CoachingStyle coachingStyle;
    private Integer dailyCalorieTarget;
    private Integer dailyProteinTarget;
    private Integer dailyCarbsTarget;
    private Integer dailyFatTarget;
    private Integer dailyWaterTargetMl;
    private SharingPreference sharingPreference;
    private String dietaryRestrictions;
    private String allergies;
    private String healthConditions;
}
