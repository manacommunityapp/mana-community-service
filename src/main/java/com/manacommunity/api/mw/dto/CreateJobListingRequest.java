package com.manacommunity.api.mw.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateJobListingRequest {

    @NotBlank
    private String title;

    private String description;
    private String employmentType;
    private String location;
    private Boolean isRemote;

    @DecimalMin("0.0")
    private BigDecimal salaryMin;

    @DecimalMin("0.0")
    private BigDecimal salaryMax;

    private String requiredSkills;

    @Min(0)
    private Integer experienceYears;

    private LocalDate expiresAt;
}
