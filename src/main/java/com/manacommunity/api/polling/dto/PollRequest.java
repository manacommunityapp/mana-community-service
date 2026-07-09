package com.manacommunity.api.polling.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class PollRequest {

    @NotBlank
    private String question;

    private String description;

    private String closesOn;

    private boolean allowMultiple;

    private boolean anonymous;

    @NotEmpty
    private List<String> options;
}
