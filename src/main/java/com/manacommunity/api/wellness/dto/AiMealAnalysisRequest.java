package com.manacommunity.api.wellness.dto;

import com.manacommunity.api.wellness.enums.WellnessEnums.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMealAnalysisRequest {
    private String text;
    private String photoBase64;
    private MealType mealType;
}
