package com.manacommunity.api.mw.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JobApplicationResponseRequest {

    @NotBlank
    private String status; // SHORTLISTED, REJECTED, OFFERED

    private String employerNote;
}
