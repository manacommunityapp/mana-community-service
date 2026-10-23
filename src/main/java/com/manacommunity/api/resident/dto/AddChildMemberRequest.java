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
public class AddChildMemberRequest {

    @NotNull(message = "flatId is required")
    private Long flatId;

    @NotBlank(message = "fullName is required")
    private String fullName;

    @NotNull(message = "dateOfBirth is required")
    private LocalDate dateOfBirth;

    @NotBlank(message = "gender is required")
    private String gender; // MALE, FEMALE, OTHER

    @NotBlank(message = "relationship is required")
    private String relationship; // SON, DAUGHTER, DEPENDENT_PARENT, OTHER

    private String emergencyContact;
    private String bloodGroup;
    private String profilePicUrl;

    @Builder.Default
    private Boolean sportsEligible = true;
}
