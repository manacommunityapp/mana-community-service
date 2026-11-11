package com.manacommunity.api.mw.dto;

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
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String requiredSkills;
    private Integer experienceYears;
    private LocalDate expiresAt;
}
