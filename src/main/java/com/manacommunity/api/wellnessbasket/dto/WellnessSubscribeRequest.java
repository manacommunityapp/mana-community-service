package com.manacommunity.api.wellnessbasket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessSubscribeRequest {

    @NotBlank(message = "Plan type is required (BASIC or PREMIUM)")
    private String planType;

    private String portionSize; // SMALL, STANDARD, LARGE

    private String goalType;    // WEIGHT_LOSS, MUSCLE_GAIN, DIABETIC_FRIENDLY, HEART_HEALTH, GENERAL_WELLNESS, KIDS_NUTRITION

    private List<String> dislikedIngredients;

    private String extraNotes;

    private String societyName;

    private String block;

    private String flatNo;
}
