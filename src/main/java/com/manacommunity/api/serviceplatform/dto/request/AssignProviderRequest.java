package com.manacommunity.api.serviceplatform.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignProviderRequest {
    @NotNull
    private Long providerId;
    private Long offeringId;
}
