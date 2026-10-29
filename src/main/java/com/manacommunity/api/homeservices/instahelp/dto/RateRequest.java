package com.manacommunity.api.homeservices.instahelp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateRequest {

    @Min(1)
    @Max(5)
    private int rating;

    private String feedback;
}
