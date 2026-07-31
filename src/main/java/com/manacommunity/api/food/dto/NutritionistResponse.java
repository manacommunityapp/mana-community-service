package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class NutritionistResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String qualification;
    private String specialization;
    private String licenseNumber;
    private Integer experienceYears;
    private String bio;
    private BigDecimal consultationFee;
    private String profileImageUrl;
    private String status;
    private BigDecimal rating;
    private Integer totalConsultations;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
