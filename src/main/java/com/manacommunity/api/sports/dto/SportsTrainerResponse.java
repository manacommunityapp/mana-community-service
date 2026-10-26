package com.manacommunity.api.sports.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SportsTrainerResponse {
    private Long id;
    private Long communityId;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String sport;
    private String certBody;
    private String certNumber;
    private Integer certYear;
    private String ageGroupsCoached;
    private String bio;
    private Integer experienceYears;
    private String profilePicUrl;
    private String status;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
}
