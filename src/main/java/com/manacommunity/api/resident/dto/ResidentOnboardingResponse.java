package com.manacommunity.api.resident.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentOnboardingResponse {
    private Long userId;
    private String fullName;
    private String phone;
    private String email;
    private Long communityId;
    private String communityName;
    private Long flatId;
    private String towerBlock;
    private String flatNumber;
    private String residentType;
    private String relationshipToFlat;
    private Boolean isPrimaryResident;
    private String verificationStatus; // PENDING, VERIFIED, REJECTED
    private String appAccessStatus;
    private String message;
    private LocalDateTime createdAt;
}
