package com.manacommunity.api.healthguard.dto;

import com.manacommunity.api.healthguard.enums.HealthGuardEnums.BloodGroup;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthProfileDto {
    private Long id;
    private Long userId;
    private BloodGroup bloodGroup;
    private String allergies;
    private String medicalConditions;
    private String emergencyNotes;
    private Integer heightCm;
    private Integer weightKg;
    private LocalDate dateOfBirth;
    private Boolean organDonorConsent;
    private String medicalIdNumber;
    private String physicianName;
    private String physicianPhone;
}
