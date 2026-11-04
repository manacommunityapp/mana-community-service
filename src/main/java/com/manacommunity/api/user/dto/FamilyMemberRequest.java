package com.manacommunity.api.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FamilyMemberRequest {

    @NotBlank(message = "Family member name is required")
    @Size(max = 150, message = "Name cannot exceed 150 characters")
    private String name;

    @Size(max = 60, message = "Relation cannot exceed 60 characters")
    private String relation;

    private Integer age;

    @JsonAlias({"dateOfBirth", "date_of_birth", "birthDate"})
    private String dob;

    private String gender;
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

    public String getEffectiveDob() {
        if (dob != null && !dob.isBlank()) return dob.trim();
        return null;
    }

    public String getEffectiveGothram() {
        if (gothram != null && !gothram.isBlank()) return gothram.trim();
        if (gotram != null && !gotram.isBlank()) return gotram.trim();
        return null;
    }
}
