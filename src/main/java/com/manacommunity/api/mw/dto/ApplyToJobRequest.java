package com.manacommunity.api.mw.dto;

import lombok.Data;

@Data
public class ApplyToJobRequest {
    private String coverNote;
    private Long passportId;
}
