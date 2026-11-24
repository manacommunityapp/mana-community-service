package com.manacommunity.api.mobility.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.service.ExpoPushService;
import com.manacommunity.platform.sqs.SqsEventEnvelope;
import com.manacommunity.platform.sqs.SqsMessageHandler;
import com.manacommunity.platform.sqs.SqsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SQS-8 consumer for {@code mana-mobility-events}.
 * Sends push notifications to drivers when a ride offer is assigned.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MobilityEventsSqsHandler implements SqsMessageHandler {

    private final ExpoPushService expoPushService;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;

    public record DriverTripOfferPayload(
            Long tripId,
            String tripRef,
            Long driverUserId,
            String pickupAddress
    ) {}

    @Override
    public String queueName() {
        return sqsProperties.getQueues().getMobilityEvents();
    }

    @Override
    public void handle(SqsEventEnvelope envelope) throws Exception {
        DriverTripOfferPayload payload = objectMapper.readValue(
                envelope.payloadJson(), DriverTripOfferPayload.class);
        deliverDriverOfferPush(payload);
    }

    public void deliverDriverOfferPush(DriverTripOfferPayload payload) {
        if (payload == null || payload.driverUserId() == null) return;
        String pickup = payload.pickupAddress() != null ? payload.pickupAddress() : "Nearby pickup";
        expoPushService.sendToUser(
                payload.driverUserId(),
                "New Ride Request",
                "Pickup: " + pickup,
                Map.of(
                        "type", "MOBILITY_TRIP_OFFER",
                        "tripId", String.valueOf(payload.tripId()),
                        "tripRef", payload.tripRef() != null ? payload.tripRef() : ""
                ),
                "default"
        );
    }
}
