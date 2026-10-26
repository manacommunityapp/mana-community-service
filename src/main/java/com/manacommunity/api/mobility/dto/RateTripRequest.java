package com.manacommunity.api.mobility.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class RateTripRequest {
    @NotNull @Min(1) @Max(5) private Integer rating;
    private List<String> tags;
    private String comment;
}
