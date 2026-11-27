package com.manacommunity.api.fulfillment.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class FulfillmentDtos {

    public record RecordEventRequest(
            String orderType,
            String eventType,
            Long actorId,
            String actorRole,
            String note,
            BigDecimal lat,
            BigDecimal lng
    ) {}

    public record AssignDeliveryRequest(
            Long deliveryPersonId,
            String deliveryPersonName,
            String deliveryPersonPhone,
            Instant estimatedArrival
    ) {}

    public record FulfillmentEventResponse(
            Long id,
            String orderId,
            String orderType,
            Long communityId,
            String eventType,
            Long actorId,
            String actorRole,
            String note,
            BigDecimal lat,
            BigDecimal lng,
            Instant occurredAt
    ) {}

    public record DeliveryStatusResponse(
            String orderId,
            Long deliveryPersonId,
            String deliveryPersonName,
            String deliveryPersonPhone,
            Instant estimatedArrival,
            String status
    ) {}

    public record OrderStatusBroadcast(
            String orderId,
            String eventType,
            Instant occurredAt
    ) {}
}
