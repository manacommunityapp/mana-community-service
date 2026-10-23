package com.manacommunity.api.resident.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPropertySummaryDto {
    private Long membershipId;
    private Long flatId;
    private String towerBlock;
    private String flatNumber;
    private Long communityId;
    private String communityName;
    private String communityCity;
    private String residentType;
    private String relationshipToFlat;
    private Boolean isPrimaryResident;
    private String verificationStatus;
    private String appAccessStatus;
    private Integer totalFamilyMembers;
}
