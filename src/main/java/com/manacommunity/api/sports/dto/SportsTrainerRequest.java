package com.manacommunity.api.sports.dto;

import lombok.Data;

@Data
public class SportsTrainerRequest {
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
}
