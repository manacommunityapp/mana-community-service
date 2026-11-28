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
 * SQS consumer for {@code mana-mobility-events}.
 *
 * <p>Handles all carpool / mobility push-notification events so that
 * blocking Expo HTTP calls are offloaded from the HTTP request thread.</p>
 *
 * <table>
 *   <tr><th>Event type</th><th>Trigger</th></tr>
 *   <tr><td>DRIVER_TRIP_OFFER</td><td>On-demand trip assigned to driver</td></tr>
 *   <tr><td>MOBILITY_GROUP_JOIN</td><td>Passenger joined driver's carpool</td></tr>
 *   <tr><td>MOBILITY_WAITLIST_PROMOTED</td><td>Waitlisted passenger promoted when seat opens</td></tr>
 *   <tr><td>MOBILITY_GROUP_LEAVE</td><td>Driver notified a passenger cancelled</td></tr>
 *   <tr><td>MOBILITY_WAITLISTED</td><td>Passenger confirmed onto waitlist</td></tr>
 *   <tr><td>MOBILITY_GROUP_CANCELLED</td><td>Passenger notified driver cancelled offer</td></tr>
 * </table>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MobilityEventsSqsHandler implements SqsMessageHandler {

    private final ExpoPushService expoPushService;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;

    // ── Payload records ────────────────────────────────────────────

    public record DriverTripOfferPayload(
            Long tripId,
            String tripRef,
            Long driverUserId,
            String pickupAddress
    ) {}

    public record GroupJoinPayload(
            Long offerId,
            Long driverUserId,
            String passengerName,
            String journeyType
    ) {}

    public record WaitlistPromotedPayload(
            Long offerId,
            Long passengerUserId,
            String origin,
            String journeyType
    ) {}

    public record GroupLeavePayload(
            Long offerId,
            Long driverUserId,
            String journeyType
    ) {}

    public record WaitlistedPayload(
            Long offerId,
            Long passengerUserId
    ) {}

    public record GroupCancelledPayload(
            Long offerId,
            Long passengerUserId
    ) {}

    // ── SqsMessageHandler ─────────────────────────────────────────

    @Override
    public String queueName() {
        return sqsProperties.getQueues().getMobilityEvents();
    }

    @Override
    public void handle(SqsEventEnvelope envelope) throws Exception {
        switch (envelope.eventType()) {
            case "DRIVER_TRIP_OFFER" ->
                    deliverDriverOfferPush(objectMapper.readValue(envelope.payloadJson(), DriverTripOfferPayload.class));
            case "MOBILITY_GROUP_JOIN" ->
                    deliverGroupJoinPush(objectMapper.readValue(envelope.payloadJson(), GroupJoinPayload.class));
            case "MOBILITY_WAITLIST_PROMOTED" ->
                    deliverWaitlistPromotedPush(objectMapper.readValue(envelope.payloadJson(), WaitlistPromotedPayload.class));
            case "MOBILITY_GROUP_LEAVE" ->
                    deliverGroupLeavePush(objectMapper.readValue(envelope.payloadJson(), GroupLeavePayload.class));
            case "MOBILITY_WAITLISTED" ->
                    deliverWaitlistedPush(objectMapper.readValue(envelope.payloadJson(), WaitlistedPayload.class));
            case "MOBILITY_GROUP_CANCELLED" ->
                    deliverGroupCancelledPush(objectMapper.readValue(envelope.payloadJson(), GroupCancelledPayload.class));
            default ->
                    log.debug("Unhandled mobility SQS event type={} eventId={}", envelope.eventType(), envelope.eventId());
        }
    }

    // ── Delivery methods ──────────────────────────────────────────

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

    public void deliverGroupJoinPush(GroupJoinPayload payload) {
        if (payload == null || payload.driverUserId() == null) return;
        String journeyType = payload.journeyType() != null ? payload.journeyType().toLowerCase() : "journey";
        String passengerName = payload.passengerName() != null ? payload.passengerName() : "A passenger";
        expoPushService.sendToUser(
                payload.driverUserId(),
                "New passenger joined",
                passengerName + " joined your " + journeyType + " journey.",
                Map.of("type", "MOBILITY_GROUP_JOIN", "offerId", String.valueOf(payload.offerId())),
                "default"
        );
    }

    public void deliverWaitlistPromotedPush(WaitlistPromotedPayload payload) {
        if (payload == null || payload.passengerUserId() == null) return;
        String journeyType = payload.journeyType() != null ? payload.journeyType().toLowerCase() : "shared";
        String origin = payload.origin() != null ? payload.origin() : "the origin";
        expoPushService.sendToUser(
                payload.passengerUserId(),
                "Your shared journey is confirmed",
                "A seat opened on the " + journeyType + " journey from " + origin + ".",
                Map.of("type", "MOBILITY_WAITLIST_PROMOTED", "offerId", String.valueOf(payload.offerId())),
                "default"
        );
    }

    public void deliverGroupLeavePush(GroupLeavePayload payload) {
        if (payload == null || payload.driverUserId() == null) return;
        String journeyType = payload.journeyType() != null ? payload.journeyType().toLowerCase() : "journey";
        expoPushService.sendToUser(
                payload.driverUserId(),
                "Passenger left the journey",
                "A seat is available again on your " + journeyType + " journey.",
                Map.of("type", "MOBILITY_GROUP_LEAVE", "offerId", String.valueOf(payload.offerId())),
                "default"
        );
    }

    public void deliverWaitlistedPush(WaitlistedPayload payload) {
        if (payload == null || payload.passengerUserId() == null) return;
        expoPushService.sendToUser(
                payload.passengerUserId(),
                "Added to journey waitlist",
                "We'll notify you if a seat opens on this journey.",
                Map.of("type", "MOBILITY_WAITLISTED", "offerId", String.valueOf(payload.offerId())),
                "default"
        );
    }

    public void deliverGroupCancelledPush(GroupCancelledPayload payload) {
        if (payload == null || payload.passengerUserId() == null) return;
        expoPushService.sendToUser(
                payload.passengerUserId(),
                "Journey cancelled",
                "The driver cancelled your shared journey offer.",
                Map.of("type", "MOBILITY_GROUP_CANCELLED", "offerId", String.valueOf(payload.offerId())),
                "default"
        );
    }
}
