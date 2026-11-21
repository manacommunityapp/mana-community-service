package com.manacommunity.api.fulfillment.service;

import com.manacommunity.api.fulfillment.dto.FulfillmentDtos.*;
import com.manacommunity.api.fulfillment.entity.DeliveryAssignment;
import com.manacommunity.api.fulfillment.entity.OrderFulfillmentEvent;
import com.manacommunity.api.fulfillment.repository.DeliveryAssignmentRepository;
import com.manacommunity.api.fulfillment.repository.OrderFulfillmentEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FulfillmentService {

    private final OrderFulfillmentEventRepository eventRepo;
    private final DeliveryAssignmentRepository deliveryRepo;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public FulfillmentEventResponse recordEvent(String orderId, String orderType, Long communityId,
                                                String eventType, Long actorId, String actorRole, String note) {
        OrderFulfillmentEvent event = OrderFulfillmentEvent.builder()
                .orderId(orderId)
                .orderType(orderType)
                .communityId(communityId)
                .eventType(eventType)
                .actorId(actorId)
                .actorRole(actorRole)
                .note(note)
                .occurredAt(Instant.now())
                .build();
        event = eventRepo.save(event);
        broadcastStatusUpdate(orderId, communityId, eventType);
        return toEventResponse(event);
    }

    @Transactional(readOnly = true)
    public List<FulfillmentEventResponse> getTimeline(String orderId) {
        return eventRepo.findByOrderIdOrderByOccurredAtAsc(orderId)
                .stream().map(this::toEventResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public String getCurrentStatus(String orderId) {
        return eventRepo.findLatestByOrderId(orderId)
                .map(OrderFulfillmentEvent::getEventType)
                .orElse("UNKNOWN");
    }

    @Transactional
    public DeliveryStatusResponse assignDelivery(String orderId, Long communityId,
                                                  Long deliveryPersonId, String name,
                                                  String phone, Instant estimatedArrival) {
        DeliveryAssignment assignment = DeliveryAssignment.builder()
                .orderId(orderId)
                .communityId(communityId)
                .deliveryPersonId(deliveryPersonId)
                .deliveryPersonName(name)
                .deliveryPersonPhone(phone)
                .estimatedArrival(estimatedArrival)
                .status("ASSIGNED")
                .build();
        assignment = deliveryRepo.save(assignment);
        recordEvent(orderId, "GENERIC", communityId, "OUT_FOR_DELIVERY", deliveryPersonId, "DELIVERY_PARTNER",
                "Delivery assigned to " + name);
        return toDeliveryResponse(assignment);
    }

    public void broadcastStatusUpdate(String orderId, Long communityId, String eventType) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/orders/" + orderId + "/status",
                    new OrderStatusBroadcast(orderId, eventType, Instant.now())
            );
        } catch (Exception e) {
            log.warn("Failed to broadcast fulfillment status for order {}: {}", orderId, e.getMessage());
        }
    }

    private FulfillmentEventResponse toEventResponse(OrderFulfillmentEvent e) {
        return new FulfillmentEventResponse(e.getId(), e.getOrderId(), e.getOrderType(),
                e.getCommunityId(), e.getEventType(), e.getActorId(), e.getActorRole(),
                e.getNote(), e.getLat(), e.getLng(), e.getOccurredAt());
    }

    private DeliveryStatusResponse toDeliveryResponse(DeliveryAssignment a) {
        return new DeliveryStatusResponse(a.getOrderId(), a.getDeliveryPersonId(),
                a.getDeliveryPersonName(), a.getDeliveryPersonPhone(),
                a.getEstimatedArrival(), a.getStatus());
    }
}
