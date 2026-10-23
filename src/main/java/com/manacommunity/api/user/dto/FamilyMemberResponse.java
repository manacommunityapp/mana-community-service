package com.manacommunity.api.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMemberResponse {
    private Long id;
    private Long userId;
    private Long communityId;
    private String name;
    private String relation;
    private Integer age;
    private String gender;
    private String dob;
    private String phone;
    private String email;
    private String bloodGroup;
    private String gothram;
    private String gotram;
    private Boolean emergencyContact;
    private Boolean isDevotee;
    private String avatar;
    private String notes;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
