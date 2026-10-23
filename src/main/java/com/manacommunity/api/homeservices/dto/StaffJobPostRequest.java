package com.manacommunity.api.homeservices.dto;

import com.manacommunity.api.homeservices.model.DomesticStaff;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class StaffJobPostRequest {

    @NotBlank
    private String title;

    @NotNull
    private DomesticStaff.StaffRole role;

    private String description;
    private String salaryRange;
    private String shiftPreference;
    private List<String> requirements;
}
