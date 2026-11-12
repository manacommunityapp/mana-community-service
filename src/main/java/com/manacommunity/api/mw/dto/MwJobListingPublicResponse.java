package com.manacommunity.api.mw.dto;

import com.manacommunity.api.mw.entity.MwJobListing;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class MwJobListingPublicResponse {

    private Long id;
    private String title;
    private String description;
    private String employmentType;
    private String location;
    private Boolean isRemote;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String requiredSkills;
    private Integer experienceYears;
    private String status;
    private LocalDate expiresAt;
    private LocalDateTime createdAt;

    public static MwJobListingPublicResponse from(MwJobListing e) {
        return MwJobListingPublicResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .description(e.getDescription())
                .employmentType(e.getEmploymentType())
                .location(e.getLocation())
                .isRemote(e.getIsRemote())
                .salaryMin(e.getSalaryMin())
                .salaryMax(e.getSalaryMax())
                .requiredSkills(e.getRequiredSkills())
                .experienceYears(e.getExperienceYears())
                .status(e.getStatus())
                .expiresAt(e.getExpiresAt())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
