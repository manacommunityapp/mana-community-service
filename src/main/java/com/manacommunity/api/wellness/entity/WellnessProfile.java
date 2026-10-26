package com.manacommunity.api.wellness.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.wellness.enums.WellnessEnums.*;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "wellness_profile", schema = "manacommunity")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessProfile extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "height_cm")
    private BigDecimal heightCm;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @Column(name = "target_weight_kg")
    private BigDecimal targetWeightKg;

    @Column(name = "age")
    private Integer age;

    @Column(name = "gender")
    private String gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level")
    private ActivityLevel activityLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type")
    private GoalType goalType;

    @Enumerated(EnumType.STRING)
    @Column(name = "coaching_style")
    private CoachingStyle coachingStyle;

    @Column(name = "daily_calorie_target")
    private Integer dailyCalorieTarget;

    @Column(name = "daily_protein_target")
    private Integer dailyProteinTarget;

    @Column(name = "daily_carbs_target")
    private Integer dailyCarbsTarget;

    @Column(name = "daily_fat_target")
    private Integer dailyFatTarget;

    @Column(name = "daily_water_target_ml")
    private Integer dailyWaterTargetMl;

    @Enumerated(EnumType.STRING)
    @Column(name = "sharing_preference")
    private SharingPreference sharingPreference;

    @Column(name = "dietary_restrictions")
    private String dietaryRestrictions;

    @Column(name = "allergies")
    private String allergies;

    @Column(name = "health_conditions")
    private String healthConditions;
}
