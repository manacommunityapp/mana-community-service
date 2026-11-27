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

    private String votingMode; // ONE_FLAT_ONE_VOTE, UDS_AREA_WEIGHTED, SHARE_CERTIFICATE

    private Integer excludeDefaultersOverDays;

    private Boolean ownersOnly;

    @NotEmpty
    private List<String> options;
}
