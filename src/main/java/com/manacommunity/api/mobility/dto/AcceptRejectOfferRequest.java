package com.manacommunity.api.mobility.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AcceptRejectOfferRequest {
    @NotNull private Boolean accept;
}
