package com.manacommunity.api.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    @JsonAlias({"name"})
    private String fullName;
    private String email;
    @JsonAlias({"mobile"})
    private String phone;
    @JsonAlias({"dateOfBirth", "dob"})
    private LocalDate dateOfBirth;
    private String gender;
    private String profilePicUrl;
    @JsonAlias({"profilePic", "profilePhoto"})
    private String profilePic;
    @JsonAlias({"flatNumber"})
    private String flatNo;
    @JsonAlias({"tower"})
    private String block;
    @JsonAlias({"block"})
    private String tower;
    private String residentType;
    private String occupancyStatus;
    private Boolean isActive;

    private String bio;
    private String profession;

    private String employeeId;
    private String govtIdType;
    private String govtIdNumber;

    @JsonAlias({"password", "newPassword"})
    private String password;
    @JsonAlias({"currentPassword", "oldPassword"})
    private String currentPassword;
}