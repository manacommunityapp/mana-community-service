package com.manacommunity.api.safety.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateIncidentRequest {
    @NotBlank private String type;
    @NotBlank private String title;
    private String description;
    private String priority;
    private String location;
    private String imageUrl;
}
