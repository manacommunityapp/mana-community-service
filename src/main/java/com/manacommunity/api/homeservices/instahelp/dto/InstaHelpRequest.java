package com.manacommunity.api.homeservices.instahelp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstaHelpRequest {

    @NotBlank
    private String serviceType;

    private String subtask;

    @NotBlank
    private String duration;

    private String notes;
}
