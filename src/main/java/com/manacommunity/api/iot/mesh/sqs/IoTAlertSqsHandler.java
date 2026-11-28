package com.manacommunity.api.iot.mesh.sqs;

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
 * SQS-5 consumer for {@code mana-iot-alerts}.
 * Sends mobile push alerts to residents when a sensor anomaly triggers,
 * decoupling Expo push HTTP calls from telemetry ingestion.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IoTAlertSqsHandler implements SqsMessageHandler {

    private final ExpoPushService expoPushService;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;

    public record IoTAnomalyPushPayload(
            Long communityId,
            Long sensorId,
            String sensorName,
            String severity,
            String description
    ) {}

    @Override
    public String queueName() {
        return sqsProperties.getQueues().getIotAlerts();
    }

    @Override
    public void handle(SqsEventEnvelope envelope) throws Exception {
        IoTAnomalyPushPayload payload = objectMapper.readValue(envelope.payloadJson(), IoTAnomalyPushPayload.class);
        deliverPush(payload);
    }

    public void deliverPush(IoTAnomalyPushPayload payload) {
        if (payload == null || payload.communityId() == null) return;
        expoPushService.sendToCommunity(
                payload.communityId(),
                "IoT Alert (" + payload.severity() + "): " + payload.sensorName(),
                payload.description(),
                Map.of(
                        "type", "IOT_ANOMALY",
                        "sensorId", String.valueOf(payload.sensorId()),
                        "severity", payload.severity() != null ? payload.severity() : "MEDIUM"
                ),
                "events"
        );
    }
}
