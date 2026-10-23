package com.manacommunity.api.resident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentOnboardingRequest {

    @NotNull(message = "communityId is required")
    private Long communityId;

    @NotBlank(message = "towerBlock is required")
    private String towerBlock;

    @NotBlank(message = "flatNumber is required")
    private String flatNumber;

    private Integer floorNumber;

    @NotBlank(message = "residentType is required (OWNER, TENANT, FAMILY_MEMBER)")
    private String residentType; // OWNER, TENANT, FAMILY_MEMBER

    private String relationshipToFlat; // SELF, SPOUSE, PARENT, CHILD, SIBLING, RELATIVE, OTHER

    private Boolean isPrimaryResident;

    // Profile Details
    @NotBlank(message = "fullName is required")
    private String fullName;

    private String email;

    private LocalDate dateOfBirth;

    private String gender; // MALE, FEMALE, OTHER

    private String profilePicUrl;

    // Optional KYC Info
    private String govtIdType;
    private String govtIdNumber;
    private String documentFrontUrl;
    private String documentBackUrl;
}
