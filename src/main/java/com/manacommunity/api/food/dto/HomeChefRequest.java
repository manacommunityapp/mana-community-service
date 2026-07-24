package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HomeChefRequest {
    @NotBlank
    private String kitchenName;
    private String description;
    private String speciality;
    private String cuisineTypes;
    private String fssaiLicense;
    private Integer maxOrdersPerDay;
    private String profileImageUrl;
    private String coverImageUrl;
}
