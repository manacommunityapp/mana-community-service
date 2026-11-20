package com.manacommunity.api.platform.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.model.NotificationCategory;
import com.manacommunity.api.model.NotificationPriority;
import com.manacommunity.api.model.NotificationType;
import com.manacommunity.api.model.ReferenceType;
import com.manacommunity.api.service.NotificationManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Central event listener consuming domain events relayed from {@link OutboxRelayScheduler}.
 * Decouples background processing and notification delivery from primary database transactions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CentralOutboxEventListener {

    private final NotificationManagementService notificationService;
    private final ObjectMapper objectMapper;

    @EventListener
    public void onOutboxEventRelayed(OutboxEventRelayed event) {
        log.info("[OutboxRelayed] Received eventType={} aggregateType={} aggregateId={} communityId={}",
                event.getEventType(), event.getAggregateType(), event.getAggregateId(), event.getCommunityId());
        try {
            switch (event.getEventType()) {
                case "VisitorCheckedIn" -> handleVisitorCheckedIn(event);
                case "VisitorCheckedOut" -> handleVisitorCheckedOut(event);
                case "VisitorOverstay" -> handleVisitorOverstay(event);
                case "GatewayPaymentCaptured" -> handlePaymentCaptured(event);
                default -> log.debug("[OutboxRelayed] Default handling for eventType={}", event.getEventType());
            }
        } catch (Exception ex) {
            log.error("[OutboxRelayed] Failed to process relayed outbox event {}: {}",
                    event.getEventType(), ex.getMessage(), ex);
        }
    }

    private void handleVisitorCheckedIn(OutboxEventRelayed event) {
        Map<String, Object> data = extractPayload(event.getPayload());
        Long residentId = getLongValue(data, "residentId");
        if (residentId != null && residentId > 0) {
            String visitorName = getStringValue(data, "visitorName", "Visitor");
            String flatNumber = getStringValue(data, "flatNumber", "");
            Long passId = getLongValue(data, "passId");

            notificationService.createNotification(
                    residentId,
                    NotificationType.VISITOR_CHECK_IN,
                    NotificationCategory.SECURITY,
                    "Visitor Arrived",
                    visitorName + " has entered the gate" + (!flatNumber.isBlank() ? " for flat " + flatNumber : "") + ".",
                    "/visitors",
                    ReferenceType.VISITOR_PASS,
                    passId,
                    NotificationPriority.HIGH,
                    null,
                    event.getCommunityId()
            );
            log.info("[OutboxRelayed] Entry notification dispatched to resident {} for visitor {}", residentId, visitorName);
        }
    }

    private void handleVisitorCheckedOut(OutboxEventRelayed event) {
        Map<String, Object> data = extractPayload(event.getPayload());
        Long residentId = getLongValue(data, "residentId");
        if (residentId != null && residentId > 0) {
            String visitorName = getStringValue(data, "visitorName", "Visitor");
            Long passId = getLongValue(data, "passId");

            notificationService.createNotification(
                    residentId,
                    NotificationType.VISITOR_CHECK_OUT,
                    NotificationCategory.SECURITY,
                    "Visitor Departed",
                    visitorName + " has checked out at the security gate.",
                    "/visitors",
                    ReferenceType.VISITOR_PASS,
                    passId,
                    NotificationPriority.LOW,
                    null,
                    event.getCommunityId()
            );
            log.info("[OutboxRelayed] Exit notification dispatched to resident {} for visitor {}", residentId, visitorName);
        }
    }

    private void handleVisitorOverstay(OutboxEventRelayed event) {
        Map<String, Object> data = extractPayload(event.getPayload());
        Long residentId = getLongValue(data, "residentId");
        if (residentId != null && residentId > 0) {
            String visitorName = getStringValue(data, "visitorName", "Visitor");
            String reason = getStringValue(data, "reason", "");
            Long passId = getLongValue(data, "passId");

            notificationService.createNotification(
                    residentId,
                    NotificationType.VISITOR_OVERSTAY_ALERT,
                    NotificationCategory.SECURITY,
                    "Visitor Overstay Alert",
                    "Visitor " + visitorName + " has exceeded their scheduled duration." + (!reason.isBlank() ? " Note: " + reason : ""),
                    "/visitors",
                    ReferenceType.VISITOR_PASS,
                    passId,
                    NotificationPriority.HIGH,
                    null,
                    event.getCommunityId()
            );
            log.info("[OutboxRelayed] Overstay alert notification dispatched to resident {}", residentId);
        }
    }

    private void handlePaymentCaptured(OutboxEventRelayed event) {
        log.info("[OutboxRelayed] Payment captured event recorded in audit pipeline for aggregateId={}",
                event.getAggregateId());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractPayload(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private String getStringValue(Map<String, Object> data, String key, String defaultValue) {
        Object val = data.get(key);
        return val != null ? String.valueOf(val) : defaultValue;
    }

    private Long getLongValue(Map<String, Object> data, String key) {
        Object val = data.get(key);
        if (val instanceof Number num) {
            return num.longValue();
        }
        if (val instanceof String str && !str.isBlank()) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
