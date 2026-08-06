package com.manacommunity.api.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WaitlistRequest {

    @NotNull
    private Long resourceId;

    @NotNull
    private String requestedDate;

    @NotNull
    private String requestedStartTime;

    @NotNull
    private String requestedEndTime;
}
