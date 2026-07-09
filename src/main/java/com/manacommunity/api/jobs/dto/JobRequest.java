package com.manacommunity.api.jobs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JobRequest {

    @NotBlank
    private String title;

    private String company;
    private String description;
    private String location;
    private String jobType;
    private String salary;
    private boolean referral;
    private String contactEmail;
}
