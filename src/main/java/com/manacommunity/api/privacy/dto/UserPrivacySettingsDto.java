package com.manacommunity.api.privacy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrivacySettingsDto {
    private Long id;
    private Long userId;
    private Boolean showPhoneToNeighbours;
    private Boolean showEmailToNeighbours;
    private Boolean showFlatInDirectory;
    private Boolean showFamilyMembers;
    private Boolean showVehicleInDirectory;
    private Boolean emergencyContactRestricted;
    private Boolean allowMarketplaceContact;
    private Boolean allowEventTagging;
    private String activityVisibility;
    private LocalDateTime updatedAt;
}
