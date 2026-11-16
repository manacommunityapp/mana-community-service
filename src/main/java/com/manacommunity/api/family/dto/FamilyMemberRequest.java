package com.manacommunity.api.family.dto;

import lombok.Data;

@Data
public class FamilyMemberRequest {
    private String name;
    private String relation;
    private String phone;
    private String dob;
    private Integer age;
    private String gender;
    private Boolean emergencyContact;
    private Boolean isMinor;
}
