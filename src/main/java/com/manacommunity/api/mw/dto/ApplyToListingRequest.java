package com.manacommunity.api.mw.dto;

import lombok.Data;

@Data
public class ApplyToListingRequest {
    private Long passportId;
    private String workerNote;
}
