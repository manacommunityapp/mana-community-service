package com.manacommunity.api.food.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class NutritionistRequest {
    private String qualification;
    private String specialization;
    private String licenseNumber;
    private Integer experienceYears;
    private String bio;
    private BigDecimal consultationFee;
    private String profileImageUrl;
}
