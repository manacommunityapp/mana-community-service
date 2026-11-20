package com.manacommunity.api.resident.household;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HouseholdResponse {

    private Long flatId;
    private String towerBlock;
    private String flatNumber;
    private Integer floorNumber;
    private Long communityId;
    private String communityName;
    private String verificationStatus;

    private List<MemberDto> adults;
    private List<DependentDto> dependents;
    private List<VehicleDto> vehicles;
    private List<StaffDto> staff;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MemberDto {
        private Long membershipId;
        private Long userId;
        private String fullName;
        private String phone;
        private String email;
        private String residentType;    // OWNER, TENANT, FAMILY_MEMBER
        private String relationship;
        private Boolean isPrimary;
        private String accessStatus;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DependentDto {
        private Long dependentId;
        private String fullName;
        private LocalDate dateOfBirth;
        private String gender;
        private String relationship;
        private Boolean sportsEligible;
        private String status;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class VehicleDto {
        private Long vehicleId;
        private String vehicleType;
        private String make;
        private String model;
        private String color;
        private String plateNumber;
        private String rfidTag;
        private String parkingSlot;
        private Boolean isDefault;
        private Boolean isActive;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StaffDto {
        private Long linkId;
        private String staffName;
        private String staffType;
        private String phone;
        private LocalDate sinceDate;
        private Boolean active;
    }
}
