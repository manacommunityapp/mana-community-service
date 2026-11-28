package com.manacommunity.api.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpressBatchCheckoutRequest {

    @NotBlank(message = "manifestCode is required")
    private String manifestCode;

    private Long communityId;
    private String gateId;

    /** Order IDs actually delivered inside the society. If null/empty and undeliveredOrderIds is provided, remaining orders are marked delivered. */
    private List<String> deliveredOrderIds;

    /** Order IDs carried back out (RTO / Undelivered). */
    private List<String> undeliveredOrderIds;
}
