package com.manacommunity.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommuteRatingRequest {
    @Min(1)
    @Max(5)
    private int score;

    @Size(max = 500)
    private String comment;

    private Long ratedUserId;
}
