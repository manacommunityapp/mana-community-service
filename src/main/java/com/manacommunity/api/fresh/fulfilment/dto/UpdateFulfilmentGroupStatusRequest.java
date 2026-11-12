package com.manacommunity.api.fresh.fulfilment.dto;

import com.manacommunity.api.fresh.fulfilment.model.FulfilmentGroupStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFulfilmentGroupStatusRequest {

    @NotNull(message = "Status is required")
    private FulfilmentGroupStatus status;

    private String trackingReference;
    private String notes;
}
