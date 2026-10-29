package com.manacommunity.api.sports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KarateProgramRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 500)
    private String description;

    private Integer minAge;
    private Integer maxAge;
    private Long startingBeltId;
    private Long targetBeltId;
}
