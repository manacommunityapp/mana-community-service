package com.manacommunity.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitVehicleReviewRequest {
    @NotNull
    @Min(1) @Max(5)
    private Integer rating;

    private String reviewText;
}
