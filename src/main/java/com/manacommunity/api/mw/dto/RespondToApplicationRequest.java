package com.manacommunity.api.mw.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RespondToApplicationRequest {

    @NotBlank
    private String decision; // ACCEPTED or REJECTED

    private String employerNote;
}
