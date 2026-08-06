package com.manacommunity.api.food.dto;

import lombok.Data;

@Data
public class AllergyRequest {
    private String allergyName;
    private String severity;
    private String notes;
}
