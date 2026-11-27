package com.manacommunity.api.healthguard.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.service.ExpoPushService;
import com.manacommunity.platform.sqs.SqsEventEnvelope;
import com.manacommunity.platform.sqs.SqsMessageHandler;
import com.manacommunity.platform.sqs.SqsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * SQS consumer for {@code mana-emergency-sos}.
 *
 * <p>Decouples HealthGuard emergency SOS push fan-outs (to emergency contacts and
 * available community responders) and 10-minute escalation alerts from the
 * HTTP request thread and scheduler loop.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmergencySosSqsHandler implements SqsMessageHandler {

    private final ExpoPushService expoPushService;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;

    public record SosTriggeredPayload(
            Long incidentId,
            String triggererName,
            String description,
            List<Long> contactUserIds,
            List<Long> responderUserIds
    ) {}

    public record SosEscalatedPayload(
            Long incidentId,
            String triggererName,
            List<Long> contactUserIds
    ) {}

    @Override
    public String queueName() {
        return sqsProperties.getQueues().getEmergencySos();
    }

    @Override
    public void handle(SqsEventEnvelope envelope) throws Exception {
        switch (envelope.eventType()) {
            case "SOS_TRIGGERED" -> deliverSosTriggered(
                    objectMapper.readValue(envelope.payloadJson(), SosTriggeredPayload.class));
            case "SOS_ESCALATED" -> deliverSosEscalated(
                    objectMapper.readValue(envelope.payloadJson(), SosEscalatedPayload.class));
            default -> log.debug("Unhandled emergency SOS SQS event type={} eventId={}",
                    envelope.eventType(), envelope.eventId());
        }
    }

    public void deliverSosTriggered(SosTriggeredPayload payload) {
        if (payload == null) return;
        String name = payload.triggererName() != null ? payload.triggererName() : "A community member";

        if (payload.contactUserIds() != null && !payload.contactUserIds().isEmpty()) {
            try {
                String body = payload.description() != null
                        ? payload.description() : "Needs immediate assistance";
                expoPushService.sendToUsers(
                        payload.contactUserIds(),
                        "🆘 SOS from " + name,
                        body,
                        Map.of("type", "SOS_ALERT", "incidentId", payload.incidentId()),
                        "emergency");
            } catch (Exception e) {
                log.warn("SOS contact push failed for incident {}: {}", payload.incidentId(), e.getMessage());
            }
        }

        if (payload.responderUserIds() != null && !payload.responderUserIds().isEmpty()) {
            try {
                String body = payload.description() != null
                        ? payload.description() : "Nearby resident needs immediate assistance";
                expoPushService.sendToUsers(
                        payload.responderUserIds(),
                        "🚨 SOS Alert — " + name,
                        body,
                        Map.of("type", "SOS_ALERT", "incidentId", payload.incidentId()),
                        "emergency");
            } catch (Exception e) {
                log.warn("SOS responder push failed for incident {}: {}", payload.incidentId(), e.getMessage());
            }
        }
    }

    public void deliverSosEscalated(SosEscalatedPayload payload) {
        if (payload == null || payload.contactUserIds() == null || payload.contactUserIds().isEmpty()) {
            return;
        }
        String name = payload.triggererName() != null ? payload.triggererName() : "A community member";
        try {
            expoPushService.sendToUsers(
                    payload.contactUserIds(),
                    "🚨 ESCALATED: SOS from " + name,
                    "No one has responded yet — please check immediately",
                    Map.of("type", "SOS_ALERT", "incidentId", payload.incidentId()),
                    "emergency");
        } catch (Exception e) {
            log.error("HealthGuard: escalation push failed for incident {}: {}",
                    payload.incidentId(), e.getMessage());
        }
    }
}
