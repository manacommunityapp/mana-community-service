package com.manacommunity.api.delivery.dto;

import com.manacommunity.api.delivery.model.DeliveryPartner;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerWebhookNotifyRequest {

    @NotBlank(message = "orderId is required")
    private String orderId;

    /**
     * e.g. ORDER_DISPATCHED, RIDER_ASSIGNED, RIDER_ARRIVED_AT_GATE, DELIVERED, CANCELLED
     */
    @NotBlank(message = "eventType is required")
    private String eventType;

    @NotNull(message = "communityId is required")
    private Long communityId;

    @NotBlank(message = "flatNumber is required")
    private String flatNumber;

    private DeliveryPartner partner;
    private String residentName;
    private String riderName;
    private String riderPhone;
    private String vehicleNumber;
    private Integer etaMinutes;
    private String itemsSummary;
}
