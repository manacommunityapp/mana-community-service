package com.manacommunity.api.wellness.enums;

public class WellnessEnums {

    public enum GoalType {
        WEIGHT_LOSS, WEIGHT_GAIN, MAINTAIN, MUSCLE_GAIN, GENERAL_WELLNESS
    }

    public enum ActivityLevel {
        SEDENTARY, LIGHTLY_ACTIVE, MODERATELY_ACTIVE, VERY_ACTIVE, EXTRA_ACTIVE
    }

    public enum MealType {
        BREAKFAST, LUNCH, DINNER, SNACK
    }

    public enum MealSourceType {
        AI_PHOTO, AI_TEXT, AI_VOICE, MANUAL, FOOD_DATABASE
    }

    public enum MealLogStatus {
        DRAFT, CONFIRMED
    }

    public enum ChallengeType {
        WALKING, HYDRATION, NUTRITION, FITNESS, YOGA, CUSTOM
    }

    public enum ChallengeStatus {
        UPCOMING, ACTIVE, COMPLETED, CANCELLED
    }

    public enum SharingPreference {
        PUBLIC, FRIENDS_ONLY, PRIVATE
    }

    public enum CoachingStyle {
        GENTLE, MOTIVATIONAL, STRICT, DATA_FOCUSED
    }

    public enum WellnessProviderType {
        DIETITIAN, NUTRITIONIST, PERSONAL_TRAINER, YOGA_INSTRUCTOR, GYM
    }

    public enum ConsentStatus {
        PENDING, ACCEPTED, DECLINED, REVOKED
    }

    public enum ConsentType {
        ACCOUNTABILITY_PARTNER, CHALLENGE_PROGRESS, WELLNESS_REPORTS
    }
}
