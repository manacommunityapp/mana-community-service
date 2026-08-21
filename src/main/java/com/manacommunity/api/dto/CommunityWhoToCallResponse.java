package com.manacommunity.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityWhoToCallResponse {

    private Long id;
    private Long communityId;
    private String department;
    private String contactPerson;
    private Long userId;
    private String userFullName;
    private String userProfilePicUrl;
    private String phoneNumber;
    private String secondaryPhone;
    private String email;
    private String designation;
    private String availability;
    private String locationOrDesk;
    private String icon;
    private String color;
    private Boolean isEmergency;
    private Integer displayOrder;
    private Boolean isActive;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
