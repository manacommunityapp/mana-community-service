package com.manacommunity.api.resident.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyRosterResponse {

    private Long flatId;
    private String towerBlock;
    private String flatNumber;
    private Long communityId;
    private String communityName;
    private String verificationStatus;

    private List<AdultMemberDto> adults;
    private List<ChildMemberDto> children;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdultMemberDto {
        private Long membershipId;
        private Long userId;
        private String fullName;
        private String phone;
        private String email;
        private String residentType;
        private String relationshipToFlat;
        private Boolean isPrimaryResident;
        private String appAccessStatus;
        private String profilePicUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChildMemberDto {
        private Long dependentId;
        private Long guardianUserId;
        private String guardianName;
        private String fullName;
        private LocalDate dateOfBirth;
        private Integer age;
        private String gender;
        private String relationship;
        private Boolean isParentManaged;
        private Boolean sportsEligible;
        private String status;
        private String profilePicUrl;
    }
}
