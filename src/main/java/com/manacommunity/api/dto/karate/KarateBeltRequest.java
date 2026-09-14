package com.manacommunity.api.dto.karate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KarateBeltRequest {

    @NotBlank
    private String name;

    private String colorHex;

    @NotNull
    @Min(1)
    private Integer rank;

    @NotNull
    @Min(0)
    private Integer minClassesRequired;

    @NotNull
    @Min(0)
    private Integer minMonthsRequired;

    private String description;

    private Long sportId;

    private Long communityId;
}
